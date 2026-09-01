#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_audio_manifest.py — BaBa Kids pre-generation tool

Run this on YOUR OWN machine (not inside the Android app). It takes the
word/phrase/name lists below and produces:

  1. audio_manifest.json — the exact file the app reads from
     app/src/main/assets/audio/audio_manifest.json. Copy the output over
     that file once you're happy with it.
  2. recording_checklist.txt — a plain list of "which text goes in which
     file", handy to give to a voice actor or feed into whatever TTS tool
     you're using (VoiceTut/Sarah run externally, a recording session,
     etc).

HONEST LIMIT: this script does NOT generate audio itself by default —
there's no reliable, truly-Egyptian-accented TTS engine this script can
call for you (see the OPTIONAL gTTS section at the bottom, which uses
Google Translate's read-aloud voice as a rough first pass ONLY, over the
internet, on your machine, at prep time — never inside the app itself,
and NOT confirmed to sound distinctly Egyptian rather than generic
Arabic). For real quality, use whatever gave you the "Sarah" sample, a
human voice actor, or any TTS tool you trust — the important part is
that the OUTPUT FILENAMES match exactly what this script assigns below,
so the app can find them.

Usage:
    python3 generate_audio_manifest.py
"""

import json
import os
import re

# ---------------------------------------------------------------------
# EDIT THESE LISTS — add/remove words, phrases, and names as you like.
# The full current phrase list from the app lives in
# BaBaKids-all-phrases.txt (delivered separately) — copy from there.
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

# Add every child's name you want pre-recorded. Each name here gets its
# OWN file — the app's dynamic sentences ("برافو يا <name>!") will still
# need those built as full phrases (see note below) since a pre-recorded
# name clip can't be spliced into a pre-recorded phrase clip cleanly.
NAMES = [
    "فارس", "آدم", "يوسف",
]


def safe_filename(index: int, prefix: str) -> str:
    return f"{prefix}_{index:03d}.wav"


def build_section(items, prefix, subfolder):
    section = {}
    checklist_lines = []
    for i, text in enumerate(items, start=1):
        filename = safe_filename(i, prefix)
        relative_path = f"{subfolder}/{filename}"
        section[text] = relative_path
        checklist_lines.append(f"{relative_path}\t{text}")
    return section, checklist_lines


def main():
    words_section, words_checklist = build_section(WORDS, "word", "words")
    phrases_section, phrases_checklist = build_section(PHRASES, "phrase", "phrases")
    names_section, names_checklist = build_section(NAMES, "name", "names")

    manifest = {
        "words": words_section,
        "phrases": phrases_section,
        "names": names_section,
    }

    out_dir = os.path.dirname(os.path.abspath(__file__))
    manifest_path = os.path.join(out_dir, "audio_manifest.json")
    checklist_path = os.path.join(out_dir, "recording_checklist.txt")

    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    with open(checklist_path, "w", encoding="utf-8") as f:
        f.write("BaBa Kids — recording checklist\n")
        f.write("Format: <file to create>    <exact text to say>\n\n")
        f.write("=== words/ ===\n")
        f.write("\n".join(words_checklist))
        f.write("\n\n=== phrases/ ===\n")
        f.write("\n".join(phrases_checklist))
        f.write("\n\n=== names/ ===\n")
        f.write("\n".join(names_checklist))
        f.write("\n")

    total = len(WORDS) + len(PHRASES) + len(NAMES)
    print(f"Wrote {manifest_path}")
    print(f"Wrote {checklist_path}")
    print(f"{total} clips to record: {len(WORDS)} words, {len(PHRASES)} phrases, {len(NAMES)} names")
    print()
    print("Next steps:")
    print("1. Record or generate each line in recording_checklist.txt,")
    print("   saving it under the exact filename shown, as a .wav file.")
    print("2. Copy those files into:")
    print("   app/src/main/assets/audio/words/")
    print("   app/src/main/assets/audio/phrases/")
    print("   app/src/main/assets/audio/names/")
    print("3. Copy audio_manifest.json over:")
    print("   app/src/main/assets/audio/audio_manifest.json")
    print("4. Rebuild the app — SmartVoiceManager finds these automatically.")


# ---------------------------------------------------------------------
# OPTIONAL: a rough first-pass auto-generator using gTTS (Google
# Translate's read-aloud voice). This is NOT confirmed to sound
# distinctly Egyptian rather than generic Arabic — treat it as a
# placeholder to test the pipeline quickly, not a final voice. Needs
# internet and `pip install gTTS` on YOUR machine — never runs inside
# the app. Uncomment generate_with_gtts() below and call it from main()
# if you want to try it.
# ---------------------------------------------------------------------

def generate_with_gtts():
    try:
        from gtts import gTTS
    except ImportError:
        print("gTTS not installed — run: pip install gTTS")
        return

    out_dir = os.path.dirname(os.path.abspath(__file__))
    all_items = [(WORDS, "word", "words"), (PHRASES, "phrase", "phrases"), (NAMES, "name", "names")]
    for items, prefix, subfolder in all_items:
        folder = os.path.join(out_dir, "generated", subfolder)
        os.makedirs(folder, exist_ok=True)
        for i, text in enumerate(items, start=1):
            clean_text = re.sub(r"[^\w\s؟!،٫٬]", "", text, flags=re.UNICODE)
            path = os.path.join(folder, safe_filename(i, prefix))
            gTTS(text=clean_text, lang="ar").save(path)
            print(f"Generated {path} — \"{text}\"")


if __name__ == "__main__":
    main()
    # generate_with_gtts()  # uncomment to also try the optional rough auto-generation
