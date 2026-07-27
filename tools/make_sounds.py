#!/usr/bin/env python3
"""Synthesises Crashy!'s sound effects and encodes them to mono OGG Vorbis.

Mono matters: Minecraft only applies 3D positional attenuation to mono samples.
"""
import os
import subprocess
import wave

import numpy as np

OUT = os.environ["OUT"]
TMP = os.environ["TMP_DIR"]
SR = 44100

rng = np.random.default_rng(20260726)


def t_axis(seconds):
    return np.linspace(0.0, seconds, int(SR * seconds), endpoint=False)


def one_pole_lowpass(x, cutoff):
    """Simple one-pole IIR; plenty for shaping noise."""
    a = np.exp(-2.0 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    prev = 0.0
    for i in range(x.size):
        prev = (1.0 - a) * x[i] + a * prev
        y[i] = prev
    return y


def one_pole_highpass(x, cutoff):
    return x - one_pole_lowpass(x, cutoff)


def noise(n):
    return rng.uniform(-1.0, 1.0, n)


def normalise(x, peak=0.92):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 1e-9 else x


def fade(x, attack=0.002, release=0.05):
    n = x.size
    a = min(int(SR * attack), n // 2)
    r = min(int(SR * release), n // 2)
    if a:
        x[:a] *= np.linspace(0.0, 1.0, a)
    if r:
        x[-r:] *= np.linspace(1.0, 0.0, r)
    return x


def write(name, samples):
    samples = fade(normalise(np.tanh(samples * 1.15)))
    pcm = (samples * 32767.0).astype(np.int16)

    wav_path = os.path.join(TMP, name + ".wav")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())

    ogg_path = os.path.join(OUT, name + ".ogg")
    os.makedirs(OUT, exist_ok=True)
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
         "-c:a", "libvorbis", "-q:a", "5", "-ac", "1", ogg_path],
        check=True)
    print("wrote", ogg_path, f"({os.path.getsize(ogg_path) // 1024} KB)")


# ------------------------------------------------------------------ impact
def impact(seed_shift, length=1.7, weight=1.0):
    t = t_axis(length)
    n = t.size

    # Body: a pitch-dropping sine, the "whump" of mass hitting mass.
    f = 34.0 + 95.0 * np.exp(-7.0 * t) * weight
    phase = 2.0 * np.pi * np.cumsum(f) / SR
    body = np.sin(phase) * np.exp(-4.2 * t) * 1.15

    # Crack: a very short bright transient right at contact.
    crack = one_pole_highpass(noise(n), 1800.0) * np.exp(-90.0 * t) * 0.85

    # Dust: broadband noise blooming out of the hit.
    dust = one_pole_lowpass(noise(n), 900.0) * np.exp(-9.0 * t) * 1.1

    # Rubble: individual chunks landing over the next second.
    rubble = np.zeros(n)
    for _ in range(14 + seed_shift % 5):
        start = rng.uniform(0.04, length * 0.72)
        i0 = int(start * SR)
        dur = rng.uniform(0.02, 0.09)
        seg = int(dur * SR)
        if i0 + seg >= n:
            continue
        env = np.exp(-np.linspace(0.0, 1.0, seg) * rng.uniform(18.0, 40.0))
        chunk = one_pole_lowpass(noise(seg), rng.uniform(700.0, 3200.0)) * env
        rubble[i0:i0 + seg] += chunk * rng.uniform(0.18, 0.5) * np.exp(-2.0 * start)

    return body + crack + dust + rubble


write("impact_1", impact(0))
write("impact_2", impact(3, length=1.5, weight=0.85))
write("impact_3", impact(7, length=1.9, weight=1.15))

# ------------------------------------------------------------------ launch
def launch():
    t = t_axis(0.75)
    n = t.size

    # Air being shoved out of the way.
    whoosh = one_pole_lowpass(noise(n), 2600.0) * np.sin(np.pi * np.clip(t / 0.45, 0, 1)) * 0.9
    whoosh *= np.exp(-2.0 * t)

    # A shove of low end so it feels heavy.
    f = 150.0 * np.exp(-9.0 * t) + 45.0
    thump = np.sin(2.0 * np.pi * np.cumsum(f) / SR) * np.exp(-7.0 * t) * 1.0

    # Faint metallic ring from the launcher itself.
    ring = (np.sin(2.0 * np.pi * 880.0 * t) * 0.25 + np.sin(2.0 * np.pi * 1319.0 * t) * 0.15)
    ring *= np.exp(-11.0 * t)

    return whoosh + thump + ring


write("launch", launch())


# ------------------------------------------------------------------ activate
def activate():
    t = t_axis(1.0)

    # Rising sweep: physics coming online.
    f = 190.0 * np.power(1000.0 / 190.0, np.clip(t / 0.7, 0, 1))
    phase = 2.0 * np.pi * np.cumsum(f) / SR
    sweep = (np.sin(phase) + 0.4 * np.sin(2.0 * phase) + 0.2 * np.sin(3.01 * phase)) * 0.55
    sweep *= np.clip(t / 0.06, 0, 1) * np.exp(-1.6 * t)

    shimmer = one_pole_highpass(noise(t.size), 4000.0) * np.exp(-3.5 * t) * 0.25

    # Landing chord once it is live.
    tail = np.zeros(t.size)
    i0 = int(0.62 * SR)
    tt = t[:t.size - i0]
    for f0, amp in ((523.25, 0.5), (784.0, 0.35), (1046.5, 0.22)):
        tail[i0:] += np.sin(2.0 * np.pi * f0 * tt) * amp * np.exp(-4.5 * tt)

    return sweep + shimmer + tail


write("activate", activate())


# ------------------------------------------------------------------ charged
def charged():
    t = t_axis(0.45)
    out = np.zeros(t.size)
    for f0, amp in ((880.0, 0.6), (1320.0, 0.35), (1760.0, 0.2)):
        out += np.sin(2.0 * np.pi * f0 * t) * amp * np.exp(-7.0 * t)
    return out * np.clip(t / 0.004, 0, 1)


write("charged", charged())
