# -*- coding: utf-8 -*-
"""
generate_with_voicetut.py — BaBa Kids audio generation using VoiceTut-TTS

WHERE TO RUN THIS: Google Colab (free), NOT this Android project, and NOT
your own machine unless you have an NVIDIA GPU with a few GB of free VRAM.
VoiceTut-TTS needs PyTorch + CUDA + a separate "OmniVoice" backbone
package — it cannot run inside a compiled Android app, on-device, at all.
That's a hard technical limit, not a missing setting.

HOW TO RUN (takes about 5 minutes to set up, free):
  1. Go to https://colab.research.google.com/ and create a new notebook.
  2. Runtime menu -> Change runtime type -> pick "T4 GPU" -> Save.
  3. Paste this whole file into one cell (or split at the "# %%" markers
     into separate cells) and run it.
  4. When it finishes, it zips everything into voicetut_output.zip and
     downloads it automatically.
  5. Unzip it. Copy the words/, phrases/, names/ folders into:
       app/src/main/assets/audio/
     and copy audio_manifest.json over the one already there.
  6. Rebuild the app — SmartVoiceManager finds these automatically
     (BundledAudioManifest, tier 1 — see the audio system report).

No coding knowledge needed beyond "paste and click Run" — this script
handles installation, generation, and packaging by itself.

WANT TO TEST A FEW PHRASES FIRST WITHOUT ANY OF THIS?
Try the live web demo, no install at all:
  https://huggingface.co/spaces/mohammedaly22/VoiceTut-TTS
"""

# %% [1] Install VoiceTut-TTS and its dependencies (Colab has a GPU + CUDA already)
# !pip install torch --index-url https://download.pytorch.org/whl/cu121
# !pip install git+https://github.com/k2-fsa/OmniVoice.git
# !pip install voicetut-tts

# %% [2] Generate every clip the app needs
import json
import os
import shutil
import zipfile

from voicetut_tts import VoiceTutTTS

# ---------------------------------------------------------------------
# EDIT THESE — same lists/format as tools/generate_audio_manifest.py, so
# filenames stay identical between both scripts. Paste more lines in from
# BaBaKids-all-phrases.txt (delivered separately) as you like.
# ---------------------------------------------------------------------

WORDS = [
    "تفاحة", "موزة", "كورة", "عربية", "كوباية", "قطة", "كلب", "بطة",
]

PHRASES = [
    "برافو!", "ممتاز!", "شاطر!", "رائع!", "أحسنت!", "يا سلام!",
    "أنت بطل!", "شاطر جدًا!", "يلا نكمل!", "يلا نجرب!",
    "حاول مرة تانية!", "قربت خالص!", "ولا يهمك!", "أنا فخور بيك!",
    "عايز أنام", "عايز آكل", "عايز أشرب", "عايز ألعب", "عايز الحمام",
    "أنا تعبان", "أنا خايف", "عايز حضن", "بطني بتوجعني",
    "عايز أطلع برا", "أنا زهقان", "عايز ماما", "عايز بابا",
    "أنا برداي", "أنا حراني", "أنا موجوع", "عايز ألبس", "خلصت",
    "عايز مساعدة", "أنا مبسوط", "بابا",
]

NAMES = [
    "فارس", "آدم", "يوسف",
]

# Pick a built-in VoiceTut speaker. Run tts.list_speakers() after loading
# (see below) to see all available names, or check the live Space's
# dropdown — "Sarah" was mentioned in the original brief as a good fit,
# but confirm it's still in the current speaker list before relying on it.
SPEAKER = "Sarah"

OUTPUT_DIR = "voicetut_output"


def safe_filename(index: int, prefix: str) -> str:
    return f"{prefix}_{index:03d}.wav"


def main():
    print("Loading VoiceTut-TTS (first run downloads the model — can take a few minutes)...")
    tts = VoiceTutTTS.from_pretrained("mohammedaly22/VoiceTut-TTS")

    # Uncomment to double check your chosen speaker actually exists:
    # print("Available speakers:", tts.list_speakers())

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    for sub in ("words", "phrases", "names"):
        os.makedirs(os.path.join(OUTPUT_DIR, sub), exist_ok=True)

    manifest = {"words": {}, "phrases": {}, "names": {}}
    all_sections = [(WORDS, "word", "words"), (PHRASES, "phrase", "phrases"), (NAMES, "name", "names")]

    total = sum(len(items) for items, _, _ in all_sections)
    done = 0

    for items, prefix, subfolder in all_sections:
        for i, text in enumerate(items, start=1):
            filename = safe_filename(i, prefix)
            out_path = os.path.join(OUTPUT_DIR, subfolder, filename)
            tts.synthesize(text, speaker=SPEAKER, output=out_path)
            manifest[subfolder][text] = f"{subfolder}/{filename}"
            done += 1
            print(f"[{done}/{total}] {subfolder}/{filename} <- \"{text}\"")

    manifest_path = os.path.join(OUTPUT_DIR, "audio_manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    zip_path = f"{OUTPUT_DIR}.zip"
    if os.path.exists(zip_path):
        os.remove(zip_path)
    shutil.make_archive(OUTPUT_DIR, "zip", OUTPUT_DIR)

    print(f"\nDone. {done} clips generated with speaker '{SPEAKER}'.")
    print(f"Download {zip_path} and follow the instructions at the top of this file.")

    # In Colab, this triggers an automatic browser download:
    try:
        from google.colab import files
        files.download(zip_path)
    except ImportError:
        pass  # not running in Colab — the zip is still saved locally


if __name__ == "__main__":
    main()
