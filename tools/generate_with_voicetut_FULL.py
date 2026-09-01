# -*- coding: utf-8 -*-
"""
generate_with_voicetut_FULL.py — BaBa Kids: generate ALL app audio with
VoiceTut-TTS in one run.

WHERE TO RUN THIS: Google Colab (free) — https://colab.research.google.com/
NOT this Android project, and NOT your own machine unless you have an
NVIDIA GPU with a few GB of free VRAM. VoiceTut-TTS needs PyTorch + CUDA +
a separate "OmniVoice" backbone package — it cannot run inside a compiled
Android app, on-device, at all. That's a hard technical limit.

HOW TO RUN:
  1. New Colab notebook -> Runtime -> Change runtime type -> T4 GPU -> Save.
  2. Paste this whole file into ONE cell and click Run (or split at the
     "# ===== STEP n =====" markers into separate cells if you'd rather
     run it step by step and catch errors early).
  3. First run downloads the model — can take a few minutes.
  4. It generates 1275 clips total, zips them, and downloads
     voicetut_output.zip automatically when done. This WILL take a while
     (over a thousand clips) — leave the tab open until it finishes.
  5. Unzip it. Copy words/, phrases/, names/ into:
       app/src/main/assets/audio/
     and copy audio_manifest.json over the one already there.
  6. Rebuild the Android app — SmartVoiceManager finds these automatically.

Want to sanity-check the voice first without any of this? Try the live
web demo, no install at all: https://huggingface.co/spaces/mohammedaly22/VoiceTut-TTS
"""

# ===== STEP 1: install VoiceTut-TTS and dependencies =====
# Standard Colab shell-magic syntax — works fine mixed into a cell with
# regular Python code below, this is the normal way Colab notebooks do it.
!pip install torch --index-url https://download.pytorch.org/whl/cu121
!pip install git+https://github.com/k2-fsa/OmniVoice.git
!pip install voicetut-tts

# ===== STEP 2: load the model =====
from voicetut_tts import VoiceTutTTS

tts = VoiceTutTTS.from_pretrained("mohammedaly22/VoiceTut-TTS")
print("الموديل جاهز")

# ===== STEP 3: check available speakers (optional but recommended) =====
print(tts.list_speakers())

# ===== STEP 4: the FULL word/phrase/name lists — everything in the app =====

# Every vocabulary word's Egyptian-Arabic spoken form (diacritized where
# the app has a spoken-form override), deduplicated. Source: WordItem.kt
# + EgyptianSpokenForms.kt in the Android project, 589 unique entries.
WORDS_AR = [
    "ياكل", "يشرب", "ينام", "يجري", "ينط", "يمشي", "يقعد", "يقف", "يلعب", "يقرا", "يكتب", "يغني",
    "يرقص", "يعوم", "يطير", "يتسلق", "يضحك", "يعيط", "يبص", "يسمع", "يتكلم", "يطبخ", "يغسل",
    "يفتح", "يقفل", "يدي", "ياخد", "يساعد", "يحضن", "كَلْب", "قُطَّة", "عُصْفور", "سَمَكَة", "أسد",
    "نمر", "فيل", "زرافة", "قرد", "دب", "أرنب", "حصان", "بقرة", "خروف", "ماعز", "فرخة", "بطة",
    "ضفدع", "سلحفاة", "تعبان", "ثعلب", "ذئب", "غزال", "جمل", "حمار وحشي", "باندا", "كوالا", "كنغر",
    "بطريق", "دولفين", "حوت", "قرش", "أخطبوط", "كابوريا", "نحلة", "فراشة", "نملة", "عنكبوت", "فأر",
    "سنجاب", "بومة", "نسر", "طاووس", "ببغاء", "حلزون", "حمامة", "جاموسة", "ديك", "وزة", "جندب",
    "يرقة", "راس", "شعر", "عيون", "مناخير", "بق", "ودان", "إيدين", "رجلين", "صوابع", "سنان",
    "لسان", "بطن", "عيد ميلاد", "تورتة", "هدية", "شمعة", "حفلة", "العيد", "رمضان", "ألعاب نارية",
    "قَميص", "حِذاء", "قُبَّعَة", "بنطلون", "شراب", "چاكيت", "فستان", "تيشيرت", "شورت", "جوانتي",
    "شال", "بيجامة", "حزام", "بلوفر", "بوت", "شبشب", "مايوه", "معطف", "نضارة", "تاج", "خاتم",
    "سوار", "قلادة", "شنطة يد", "كاب", "أَحْمَر", "بُرْتُقالي", "أَصْفَر", "أَخْضَر", "أَزْرَق",
    "بَنَفْسَجي", "بُنّي", "أَسْوَد", "أَبْيَض", "رمادي", "دهبي", "فضي", "وردي", "تركواز", "بيج",
    "كحلي", "نعناعي", "شمال", "يمين", "قدام", "ورا", "بين", "جوه", "برا", "نص", "عصير", "شاي",
    "قهوة", "ليموناضة", "ميه غازية", "شوكولاتة سخنة", "سموذي", "ميلك شيك", "ميه جوز هند",
    "عصير مانجة", "كمبيوتر", "تابلت", "سماعات", "شاحن", "كيبورد", "پرينتر", "سماعة", "مامَا",
    "بابَا", "أُخْتي", "أَخويا", "تيتة", "جدو", "خالتي", "عمو", "ابن عمي", "بيبي", "صاحبي", "جوز",
    "مراة", "ابني", "بنتي", "توأم", "عيلة", "مزرعة", "حظيرة", "قش", "خيال المآتة", "بئر", "سور",
    "طاحونة هوا", "سَعيد", "زَعْلان", "غَضْبان", "نَعْسان", "خايِف", "بَحِبّ", "تَعْبان", "متحمس",
    "زهقان", "متفاجئ", "خجول", "فخور", "مش فاهم", "قلقان", "شجاع", "هادئ", "غيران", "جعان",
    "عطشان", "مريض", "واثق", "محرج", "فرحان", "مرتاح", "متضايق", "تُفّاحَة", "مَوْزَة", "لَبَن",
    "بيتْزا", "مَيَّة", "بيضة", "جبنة", "عيش", "عسل", "زبدة", "رز", "مكرونة", "شوربة", "سلطة",
    "كشري", "برتقالة", "عنب", "فراولة", "بطيخة", "مانجة", "أناناس", "خوخة", "كمثرى", "كريز",
    "ليمونة", "كيوي", "جوز هند", "تين", "برقوق", "جوافة", "رمانة", "شمام", "مشمش", "بلح",
    "أفوكادو", "سَرير", "باب", "كُرْسي", "ترابيزة", "كنبة", "شباك", "لمبة", "مراية", "ساعة",
    "تليفزيون", "تليفون", "مفتاح", "تلاجة", "فرن", "حوض", "بانيو", "فوطة", "مخدة", "بطانية",
    "سجادة", "سلم", "مطبخ", "حمام", "جنينة", "بلكونة", "ستارة", "مروحة", "تكييف", "بوتاجاز",
    "غسالة", "مكواة", "دولاب", "رف", "دُكْتور", "مُعَلِّم", "شُرْطي", "رَجُل إِطْفاء", "طَبّاخ",
    "مُزارِع", "طَيّار", "مُمَرِّضَة", "مهندس", "فنان", "مغني", "راقصة", "سواق", "جرسون", "حلاق",
    "خياط", "صياد", "جندي", "عالم", "رياضي", "مدرب", "مصمم", "مترجم", "مبرمج", "بائع", "خشب",
    "معدن", "زجاج", "بلاستيك", "ورق", "قطن", "جلد", "حجر", "جيتار", "بيانو", "طبول", "كمان",
    "فلوت", "ترومبيت", "غنا", "ميكروفون", "راديو", "أغنية", "شَمْس", "شَجَرَة", "وَرْدَة", "قمر",
    "سما", "سحابة", "مطر", "تلج", "هوا", "جبل", "نهر", "بحر", "شاطئ", "غابة", "صحرا", "قوس قزح",
    "رعد", "برق", "ورقة شجر", "عشب", "صخرة", "رمل", "نار", "جليد", "بركان", "جزيرة", "بحيرة",
    "شلال", "كهف", "تراب", "عش", "وادي", "شروق", "غروب", "واحِد", "اِتْنين", "تَلاتَة",
    "أَرْبَعَة", "خَمْسَة", "سِتَّة", "سَبْعَة", "تَمانْيَة", "تِسْعَة", "عَشَرَة", "حداشر",
    "اتناشر", "تلتاشر", "اربعتاشر", "خمستاشر", "سطاشر", "سبعتاشر", "تمنتاشر", "تسعتاشر", "عشرين",
    "خمسة وعشرين", "تلاتين", "خمسين", "ميه", "دكتور أسنان", "دكتور بيطري", "قاضي", "محامي",
    "محاسب", "كهربائي", "سباك", "نجار", "مصور", "صحفي", "كبير", "صغير", "سخن", "بارد", "سريع",
    "بطيء", "فوق", "تحت", "مليان", "فاضي", "مبلول", "ناشف", "قريب", "بعيد", "تقيل", "خفيف", "قديم",
    "جديد", "نضيف", "وسخ", "قلم رصاص", "قلم حبر", "كتاب", "شنطة", "مسطرة", "استيكة", "مقص", "صمغ",
    "كراسة", "ألوان", "مكتب", "سبورة", "فصل", "الربيع", "الصيف", "الخريف", "الشتا", "دائِرَة",
    "مُرَبَّع", "مُثَلَّث", "نَجْمَة", "قَلْب", "مُعَيَّن", "بيضاوي", "مستطيل", "مكعب", "خماسي",
    "سداسي", "مخروط", "سهم", "هلال", "حلزوني", "فلوس", "محل", "سوق", "عربية تسوق", "سعر",
    "عايز أنام", "عايز آكل", "عايز أشرب", "عايز ألعب", "عايز الحمام", "أنا تعبان", "أنا خايف",
    "عايز حضن", "بطني بتوجعني", "عايز أطلع برا", "أنا زهقان", "عايز ماما", "عايز بابا",
    "أنا برداي", "أنا حراني", "أنا موجوع", "عايز ألبس", "خلصت", "عايز مساعدة", "أنا مبسوط", "كوكب",
    "الأرض", "المريخ", "رائد فضاء", "سفينة فضاء", "مجرة", "مذنب", "قمر صناعي", "تليسكوب",
    "كائن فضائي", "كورة قدم", "كرة سلة", "سباحة", "جري", "ركوب عجل", "تنس", "جون", "سباق", "جمباز",
    "كاراتيه", "تزلج", "ملاكمة", "كرة طائرة", "تنس طاولة", "الأحد", "الإتنين", "التلات", "الأربع",
    "الخميس", "الجمعة", "السبت", "الصبح", "الضهر", "الليل", "النهارده", "بكرة", "إمبارح", "شاكوش",
    "مفك", "قفل", "شمسية", "محفظة", "كاميرا", "ساعة يد", "كشاف", "حبل", "سلم نقال", "مكنسة",
    "جردل", "كورَة", "عَروسَة", "عَرَبِيَّة لَعِب", "طيارة ورق", "مكعبات", "بازل", "دبدوب",
    "روبوت", "يويو", "بالونة", "سكيت بورد", "حبل نط", "فقاقيع", "صلصال", "درون", "بيت عرايس",
    "نطاطة", "طقم مطبخ لعب", "دمية أكشن", "فرفوطة", "عَرَبِيَّة", "أوتوبيس", "عَجَلَة", "قطر",
    "طيارة", "مركب", "سفينة", "موتوسيكل", "عربية نقل", "تاكسي", "إسعاف", "عربية إطفاء", "هليكوبتر",
    "جرار", "غواصة", "صاروخ", "سكوتر", "مترو", "ميكروباص", "فلوكة", "منطاد", "طماطم", "بطاطس",
    "جزر", "خيار", "بصلة", "توم", "فلفل", "باذنجان", "ذرة", "بسلة", "خس", "قرع", "بروكلي", "كرنب",
    "مشروم", "فاصوليا", "فجل", "قرنبيط", "مشمس", "ماطر", "غايم", "فيه هوا", "فيه تلج", "حر", "برد",
    "شبورة", "عاصف",
]

# Every vocabulary word's English form, deduplicated. 587 unique entries.
WORDS_EN = [
    "Eat", "Drink", "Sleep", "Run", "Jump", "Walk", "Sit", "Stand", "Play", "Read", "Write",
    "Sing", "Dance", "Swim", "Fly", "Climb", "Laugh", "Cry", "Look", "Listen", "Talk", "Cook",
    "Wash", "Open", "Close", "Give", "Take", "Help", "Hug", "Dog", "Cat", "Bird", "Fish", "Lion",
    "Tiger", "Elephant", "Giraffe", "Monkey", "Bear", "Rabbit", "Horse", "Cow", "Sheep", "Goat",
    "Chicken", "Duck", "Frog", "Turtle", "Snake", "Fox", "Wolf", "Deer", "Camel", "Zebra", "Panda",
    "Koala", "Kangaroo", "Penguin", "Dolphin", "Whale", "Shark", "Octopus", "Crab", "Bee",
    "Butterfly", "Ant", "Spider", "Mouse", "Squirrel", "Owl", "Eagle", "Peacock", "Parrot",
    "Snail", "Pigeon", "Buffalo", "Rooster", "Goose", "Grasshopper", "Caterpillar", "Head", "Hair",
    "Eyes", "Nose", "Mouth", "Ears", "Hands", "Feet", "Fingers", "Teeth", "Tongue", "Belly",
    "Birthday", "Cake", "Gift", "Candle", "Party", "Eid", "Ramadan", "Fireworks", "Shirt", "Shoes",
    "Hat", "Pants", "Socks", "Jacket", "Dress", "T-shirt", "Shorts", "Gloves", "Scarf", "Pajamas",
    "Belt", "Sweater", "Boots", "Sandals", "Swimsuit", "Coat", "Glasses", "Crown", "Ring",
    "Bracelet", "Necklace", "Handbag", "Cap", "Red", "Orange", "Yellow", "Green", "Blue", "Purple",
    "Brown", "Black", "White", "Gray", "Gold", "Silver", "Pink", "Turquoise", "Beige", "Navy",
    "Mint", "Left", "Right", "Front", "Behind", "Between", "Inside", "Outside", "Middle", "Juice",
    "Tea", "Coffee", "Lemonade", "Soda", "Hot Chocolate", "Smoothie", "Milkshake", "Coconut Water",
    "Mango Juice", "Computer", "Tablet", "Headphones", "Charger", "Keyboard", "Printer", "Speaker",
    "Mom", "Dad", "Sister", "Brother", "Grandma", "Grandpa", "Aunt", "Uncle", "Cousin", "Baby",
    "Friend", "Husband", "Wife", "Son", "Daughter", "Twin", "Family", "Farm", "Barn", "Hay",
    "Scarecrow", "Well", "Fence", "Windmill", "Happy", "Sad", "Angry", "Sleepy", "Scared", "Love",
    "Tired", "Excited", "Bored", "Surprised", "Shy", "Proud", "Confused", "Worried", "Brave",
    "Calm", "Jealous", "Hungry", "Thirsty", "Sick", "Confident", "Embarrassed", "Joyful",
    "Relieved", "Annoyed", "Apple", "Banana", "Milk", "Pizza", "Water", "Egg", "Cheese", "Bread",
    "Honey", "Butter", "Rice", "Pasta", "Soup", "Salad", "Koshary", "Grapes", "Strawberry",
    "Watermelon", "Mango", "Pineapple", "Peach", "Pear", "Cherry", "Lemon", "Kiwi", "Coconut",
    "Fig", "Plum", "Guava", "Pomegranate", "Melon", "Apricot", "Date", "Avocado", "Bed", "Door",
    "Chair", "Table", "Sofa", "Window", "Lamp", "Mirror", "Clock", "TV", "Phone", "Key", "Fridge",
    "Oven", "Sink", "Bathtub", "Towel", "Pillow", "Blanket", "Carpet", "Stairs", "Kitchen",
    "Bathroom", "Garden", "Balcony", "Curtain", "Fan", "AC", "Stove", "Washing Machine", "Iron",
    "Closet", "Shelf", "Doctor", "Teacher", "Police Officer", "Firefighter", "Chef", "Farmer",
    "Pilot", "Nurse", "Engineer", "Artist", "Singer", "Dancer", "Driver", "Waiter", "Barber",
    "Tailor", "Fisherman", "Soldier", "Scientist", "Athlete", "Coach", "Designer", "Translator",
    "Programmer", "Salesperson", "Wood", "Metal", "Glass", "Plastic", "Paper", "Cotton", "Leather",
    "Stone", "Guitar", "Piano", "Drums", "Violin", "Flute", "Trumpet", "Singing", "Microphone",
    "Radio", "Song", "Sun", "Tree", "Flower", "Moon", "Sky", "Cloud", "Rain", "Snow", "Wind",
    "Mountain", "River", "Sea", "Beach", "Forest", "Desert", "Rainbow", "Thunder", "Lightning",
    "Leaf", "Grass", "Rock", "Sand", "Fire", "Ice", "Volcano", "Island", "Lake", "Waterfall",
    "Cave", "Soil", "Nest", "Valley", "Sunrise", "Sunset", "One", "Two", "Three", "Four", "Five",
    "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
    "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty", "Twenty-Five", "Thirty", "Fifty",
    "One Hundred", "Dentist", "Vet", "Judge", "Lawyer", "Accountant", "Electrician", "Plumber",
    "Carpenter", "Photographer", "Journalist", "Big", "Small", "Hot", "Cold", "Fast", "Slow", "Up",
    "Down", "Full", "Empty", "Wet", "Dry", "Near", "Far", "Heavy", "Light", "Old", "New", "Clean",
    "Dirty", "Pencil", "Pen", "Book", "Bag", "Ruler", "Eraser", "Scissors", "Glue", "Notebook",
    "Crayons", "Desk", "Board", "Classroom", "Spring", "Summer", "Autumn", "Winter", "Circle",
    "Square", "Triangle", "Star", "Heart", "Diamond", "Oval", "Rectangle", "Cube", "Pentagon",
    "Hexagon", "Cone", "Arrow", "Crescent", "Spiral", "Money", "Coin", "Shop", "Market", "Cart",
    "Price", "I want to sleep", "I want to eat", "I want to drink", "I want to play",
    "I need the bathroom", "I'm tired", "I'm scared", "I want a hug", "My tummy hurts",
    "I want to go outside", "I'm bored", "I want mommy", "I want daddy", "I'm cold", "I'm hot",
    "Something hurts", "I want to get dressed", "I'm done", "I need help", "I'm happy", "Planet",
    "Earth", "Mars", "Astronaut", "Spaceship", "Galaxy", "Comet", "Satellite", "Telescope",
    "Alien", "Football", "Basketball", "Swimming", "Running", "Cycling", "Tennis", "Goal", "Race",
    "Gymnastics", "Karate", "Skating", "Boxing", "Volleyball", "Ping Pong", "Sunday", "Monday",
    "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Morning", "Noon", "Night", "Today",
    "Tomorrow", "Yesterday", "Hammer", "Screwdriver", "Lock", "Umbrella", "Wallet", "Camera",
    "Watch", "Flashlight", "Rope", "Ladder", "Broom", "Bucket", "Ball", "Doll", "Toy car", "Kite",
    "Blocks", "Puzzle", "Teddy Bear", "Robot", "Yo-yo", "Balloon", "Skateboard", "Jump Rope",
    "Bubbles", "Playdough", "Drone", "Dollhouse", "Pogo Stick", "Kitchen Playset", "Action Figure",
    "Spinning Top", "Car", "Bus", "Bike", "Train", "Plane", "Boat", "Ship", "Motorcycle", "Truck",
    "Taxi", "Ambulance", "Fire Truck", "Helicopter", "Tractor", "Submarine", "Rocket", "Scooter",
    "Metro", "Microbus", "Felucca", "Hot Air Balloon", "Tomato", "Potato", "Carrot", "Cucumber",
    "Onion", "Garlic", "Pepper", "Eggplant", "Corn", "Peas", "Lettuce", "Pumpkin", "Broccoli",
    "Cabbage", "Mushroom", "Beans", "Radish", "Cauliflower", "Sunny", "Rainy", "Cloudy", "Windy",
    "Snowy", "Foggy", "Stormy",
]

# The Arabic alphabet (28 letters).
LETTERS_AR = [
    "ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ",
    "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي",
]

# The English alphabet (26 letters).
LETTERS_EN = [
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
    "T", "U", "V", "W", "X", "Y", "Z",
]

# Every fixed phrase in the app: generic (name-free) encouragement lines
# using the same "بطل"/"بطلة" fallback address the app itself uses when no
# child name is set, plus the app's short exclamations and "بابا".
# 37 entries. (Full phrase list, including per-gender
# variants, is also in BaBaKids-all-phrases.txt.)
PHRASES = [
    "🌟 شاطر يا بطل!", "👏 برافو يا بطل!", "🔥 جامد يا بطل!", "⭐ تحفة يا بطل!", "😍 الله عليك يا بطل!",
    "👍 معلم يا بطل!", "🌟 شاطرة يا بطلة!", "👏 برافو يا بطلة!", "🔥 جامدة يا بطلة!",
    "⭐ تحفة يا بطلة!", "😍 الله عليكي يا بطلة!", "👍 معلمة يا بطلة!", "😊 حاول تاني يا بطل",
    "❤️ قريب أوي يا بطل!", "يلا كمان مرة يا بطل ❤️", "معلش يا بطل، جرب تاني",
    "كمّل يا بطل، هتعرفها!", "😊 حاولي تاني يا بطلة", "❤️ قريبة أوي يا بطلة!",
    "يلا كمان مرة يا بطلة ❤️", "معلش يا بطلة، جربي تاني", "كمّلي يا بطلة، هتعرفيها!", "برافو!",
    "ممتاز!", "شاطر!", "رائع!", "أحسنت!", "يا سلام!", "أنت بطل!", "شاطر جدًا!", "يلا نكمل!",
    "يلا نجرب!", "حاول مرة تانية!", "قربت خالص!", "ولا يهمك!", "أنا فخور بيك!", "بابا",
]

# Sample child names — edit this to your own child's name(s). Each name
# here gets its own clip; the app's dynamic "برافو يا <name>!" sentences
# still need the FULL sentence recorded per name (see the audio system
# report for why a name clip can't be spliced into a phrase clip).
NAMES = [
    "فارس", "آدم", "يوسف", "مريم", "ياسمين", "علي", "حسن", "سارة",
]

SPEAKER = "Sarah"  # change if this isn't in the list printed in STEP 3

# ===== STEP 5: generate everything =====
import os

OUTPUT_DIR = "voicetut_output"

def safe_filename(index, prefix):
    return f"{prefix}_{index:03d}.wav"

os.makedirs(OUTPUT_DIR, exist_ok=True)
for sub in ("words", "phrases", "names"):
    os.makedirs(os.path.join(OUTPUT_DIR, sub), exist_ok=True)

manifest = {"words": {}, "phrases": {}, "names": {}}

# Arabic + English words, and both alphabets, all live under words/.
all_words = WORDS_AR + WORDS_EN + LETTERS_AR + LETTERS_EN
all_sections = [
    (all_words, "word", "words"),
    (PHRASES, "phrase", "phrases"),
    (NAMES, "name", "names"),
]

total = sum(len(items) for items, _, _ in all_sections)
done = 0
failed = []

for items, prefix, subfolder in all_sections:
    for i, text in enumerate(items, start=1):
        filename = safe_filename(i, prefix)
        out_path = os.path.join(OUTPUT_DIR, subfolder, filename)
        try:
            tts.synthesize(text, speaker=SPEAKER, output=out_path)
            manifest[subfolder][text] = f"{subfolder}/{filename}"
        except Exception as e:
            failed.append((text, str(e)))
        done += 1
        if done % 10 == 0 or done == total:
            print(f"[{done}/{total}] latest: {subfolder}/{filename} <- \"{text}\"")

print(f"\nGenerated {done - len(failed)} / {total} clips.")
if failed:
    print(f"{len(failed)} failed (kept going, these just weren't saved):")
    for text, err in failed[:20]:
        print(f"  - \"{text}\": {err}")

# ===== STEP 6: save the manifest and download everything =====
import json as _json
import shutil

manifest_path = os.path.join(OUTPUT_DIR, "audio_manifest.json")
with open(manifest_path, "w", encoding="utf-8") as f:
    _json.dump(manifest, f, ensure_ascii=False, indent=2)

zip_path = f"{OUTPUT_DIR}.zip"
if os.path.exists(zip_path):
    os.remove(zip_path)
shutil.make_archive(OUTPUT_DIR, "zip", OUTPUT_DIR)

print(f"\nDone. Downloading {zip_path} now...")

try:
    from google.colab import files
    files.download(zip_path)
except ImportError:
    print("Not running in Colab — the zip is saved locally at", zip_path)
