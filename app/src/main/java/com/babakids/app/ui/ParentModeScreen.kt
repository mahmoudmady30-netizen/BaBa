package com.babakids.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.babakids.app.data.AppData
import com.babakids.app.data.CustomRewardsRepository
import com.babakids.app.data.CustomWordsRepository
import com.babakids.app.data.MediaStorage
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.RewardManager
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import kotlinx.coroutines.launch

/**
 * All the dashboard's display text in one place, switched by the
 * app-language setting — so the dashboard follows the same Arabic/English
 * choice as the child-facing screens instead of staying Arabic-only.
 */
private class ParentStrings(val english: Boolean) {
    val gateTitle = if (english) "🔒 Parent Area" else "🔒 منطقة الوالدين"
    val enterPinLabel = if (english) "Enter PIN" else "ادخل الرقم السري"
    val wrongPin = if (english) "Wrong PIN, try again" else "الرقم غلط، جرب تاني"
    val enter = if (english) "Enter" else "دخول"
    val defaultPinHint = if (english)
        "💡 The default PIN is 1234 unless you've changed it."
    else
        "💡 الرقم السري الافتراضي هو 1234 لو لسه ما غيّرتوش."

    val dashboardTitle = if (english) "Parent Dashboard" else "لوحة الوالدين"
    val developerLabel = if (english) "Developer" else "المطوّر"
    val basicSettingsTab = if (english) "⚙️ Settings" else "⚙️ الإعدادات"
    val advancedSettingsTab = if (english) "🔧 Advanced" else "🔧 متقدم"

    val progressTitle = if (english) "📊 Progress" else "📊 متابعة التقدم"
    val totalStars = if (english) "⭐ Total stars: " else "⭐ إجمالي النجوم: "
    val totalAttempts = if (english) "🎤 Total speech attempts: " else "🎤 إجمالي محاولات النطق: "
    val streakLabel = if (english) "🔥 Day streak: " else "🔥 أيام متتالية: "
    val practicedLabel = if (english) "📚 Distinct words practiced: " else "📚 عدد الكلمات المختلفة اللي اتدربت عليها: "
    val wordsAvailable = if (english) "🔤 Words available: " else "🔤 عدد الكلمات المتاحة: "
    val youAdded = if (english) "you added " else "أضفتها بنفسك "

    val dailyLimitTitle = if (english) "⏱️ Daily usage time" else "⏱️ مدة الاستخدام اليومية"
    val minutesPerDay = if (english) "Minutes per day" else "دقيقة في اليوم"
    val save = if (english) "Save" else "حفظ"
    val zeroForNoLimit = if (english) "Enter 0 for no limit." else "اكتب 0 عشان تلغي الحد اليومي."

    val accessibilityTitle = if (english) "⚙️ General settings" else "⚙️ الوظائف العامة"
    val reduceMotion = if (english) "Reduce motion and effects" else "تقليل الحركة والمؤثرات"
    val hapticFeedbackLabel = if (english) "Vibration on tap and correct answers" else "اهتزاز عند اللمس والإجابة الصح"
    val wordEditModeLabel = if (english)
        "✏️ Word Edit Mode (pencil icon to change pictures/pronunciation)"
    else
        "✏️ وضع تعديل الكلمات (أيقونة قلم لتغيير الصور والنطق)"
    val voiceEnabledLabel = if (english) "Voice enabled" else "الصوت شغال"
    val autoSpeakLabel = if (english) "Auto-speak words on open" else "نطق الكلمة تلقائيًا عند الفتح"
    val voiceVolumeLabel = if (english) "Voice volume" else "مستوى صوت النطق"
    val clearVoiceCacheHint = if (english)
        "The app saves generated voice clips so they play instantly next time, even offline."
    else
        "التطبيق بيحفظ مقاطع الصوت اللي بيتولّدها عشان تشتغل فورًا المرة الجاية، حتى من غير نت."
    val clearVoiceCacheButton = if (english) "🗑️ Clear saved voices" else "🗑️ مسح الأصوات المحفوظة"
    val voiceCacheClearedMessage = if (english) "Cleared ✅" else "اتمسحت ✅"
    val voiceDiagnosticsTitle = if (english) "🔍 Voice Debug" else "🔍 تشخيص الصوت"
    val voiceDiagnosticsHint = if (english)
        "This app runs fully offline — no online voice service is used. Type any text below and tap Test to see exactly which tier answered it: bundled audio (real pre-recorded clips), local cache, or the device's fallback voice."
    else
        "التطبيق ده شغال أوفلاين بالكامل — مفيش أي خدمة صوت أونلاين بتتستخدم. اكتب أي نص تحت ودوس اختبار عشان تشوف بالظبط مين رد عليه: صوت مسجل مسبقًا (Bundled)، الكاش المحلي، أو صوت الجهاز الاحتياطي."
    val voiceDiagnosticsNone = if (english)
        "No voice attempt yet — try a word or wait for a celebration."
    else
        "لسه مفيش محاولة نطق — جرب كلمة أو استنى احتفال."
    val voiceTestButton = if (english) "🧪 Test this text" else "🧪 جرب النص ده"
    val voiceDebugInputLabel = if (english) "Text to test" else "النص اللي عايز تجربه"
    val bundledAudioCountLabel = if (english) "Bundled pre-recorded clips available:" else "عدد الأصوات المسجلة مسبقًا المتاحة:"
    val generateAllTitle = if (english) "🪄 Auto-generate all offline audio" else "🪄 توليد كل الأصوات تلقائيًا"
    val generateAllHint = if (english)
        "Generates and caches every word and phrase in the app using the device's own voice, right on the phone — no internet, no manual files. This makes all of them play instantly afterward, fully offline. This covers 1000+ clips and can take several minutes — stay on this screen until it finishes, or it'll stop partway. Honest note: this still uses the same device voice as the fallback, not a more natural one — for that, real pre-recorded audio is still needed (see the bundled-audio guide)."
    else
        "بيولّد ويحفظ كل كلمة وجملة في التطبيق بصوت الجهاز نفسه، على الموبايل مباشرة — من غير نت ومن غير ما تحط أي ملفات يدويًا. كده كل الكلمات دي هتتقال فورًا بعد كده، أوفلاين بالكامل. ده أكتر من 1000 مقطع ومكن ياخد كذا دقيقة — خليك في الشاشة دي لحد ما يخلص، لو خرجت هيوقف في النص. ملاحظة صادقة: ده لسه بيستخدم نفس صوت الجهاز الاحتياطي مش صوت أطبع — عشان صوت أطبع فعلي لازم أصوات حقيقية متسجلة مسبقًا (شوف دليل الأصوات المسجلة)."
    val generateAllButton = if (english) "Start generating now" else "ابدأ التوليد دلوقتي"
    val contentControlTitle = if (english) "🎯 Content Control" else "🎯 التحكم في المحتوى"
    val contentControlHint = if (english)
        "Turn off whole categories or individual words so the child only sees what you choose — helps avoid overwhelm. Tap 🤍 next to a word to also feature it in \"My Words\"."
    else
        "اقفل أقسام كاملة أو كلمات معينة عشان الطفل يشوف بس اللي انت مختاره — بيقلل التشتت. دوس على 🤍 جنب أي كلمة عشان تظهر كمان في \"كلماتي\"."
    val wordsInCategoryLabel = if (english) "Words in this category:" else "الكلمات في القسم ده:"
    val imagePickErrorMsg = if (english)
        "Couldn't read that photo — try a different one." else "معرفتش أفتح الصورة دي — جرب صورة تانية."
    val recordingErrorMsg = if (english)
        "Couldn't start recording — check mic permission and try again."
    else
        "معرفتش أبدأ التسجيل — تأكد من إذن المايك وجرب تاني."
    val rewardsTitle = if (english) "🎁 Real-World Rewards" else "🎁 مكافآت حقيقية"
    val rewardsHint = if (english)
        "Set up real rewards for reaching a star count — ice cream, an outing, help with something. The child gets a special popup the moment they earn it."
    else
        "حدد مكافآت حقيقية لما يوصل لعدد نجوم معين — آيس كريم، خروجة، مساعدة في حاجة. الطفل هيشوف popup خاص لحظة ما يكسبها."
    val rewardTitleLabel = if (english) "Reward (e.g. Ice cream trip)" else "المكافأة (مثلاً: نروح ناكل آيس كريم)"
    val rewardStarsLabel = if (english) "Stars needed" else "عدد النجوم المطلوب"
    val addRewardButton = if (english) "Add reward" else "إضافة المكافأة"
    val yourRewardsTitle = if (english) "Your rewards" else "المكافآت اللي ضفتها"
    val achievedOnLabel = if (english) "🎉 Achieved: " else "🎉 اتحققت: "

    val childNameTitle = if (english) "👶 Child's name" else "👶 اسم الطفل"
    val childNameHint = if (english)
        "Used in every encouragement message, e.g. \"Great job, Faris!\" instead of \"Great job!\""
    else
        "بيتقال في كل رسائل التشجيع، مثلاً \"برافو يا فارس!\" بدل \"برافو!\""
    val childNameLabel = if (english) "Child's name" else "اسم الطفل"
    val childGenderLabel = if (english) "Child's gender:" else "جنس الطفل:"
    val boy = if (english) "Boy 👦" else "ولد 👦"
    val girl = if (english) "Girl 👧" else "بنت 👧"
    val genderHint = if (english)
        "Used for the fallback address (\"Champ\") when no name is set."
    else
        "بيستخدم في صيغة التشجيع (\"بطل\"/\"بطلة\") لو الاسم مش متسجل."

    val languageTitle = if (english) "🌐 App language" else "🌐 لغة التطبيق"
    val arabic = "العربية"
    val english_ = "English"
    val languageHint = if (english)
        "Changes words, letters, speech, and encouragement in the child's screens (including this dashboard)."
    else
        "بتغيّر الكلمات والحروف والنطق والتشجيع في صفحات الطفل (وفي لوحة الوالدين دي كمان)."

    val dialectTitle = if (english) "🗣️ Arabic style" else "🗣️ أسلوب العربي"
    val dialectEgyptian = if (english) "Egyptian" else "عامية مصرية"
    val dialectFusha = if (english) "Modern Standard (Fusha)" else "فصحى"
    val dialectHint = if (english)
        "Only applies when the app language is Arabic — affects the voice and encouragement wording."
    else
        "بتظهر بس لما لغة التطبيق عربي — بتتحكم في نبرة الصوت وصياغة التشجيع."

    val changePinTitle = if (english) "🔑 Change PIN" else "🔑 تغيير الرقم السري"
    val currentPinLabel = if (english) "Current PIN" else "الرقم السري الحالي"
    val newPinLabel = if (english) "New PIN (4 digits)" else "الرقم السري الجديد (4 أرقام)"
    val confirmPinLabel = if (english) "Confirm new PIN" else "تأكيد الرقم السري الجديد"
    val pinWrongCurrent = if (english) "Current PIN is wrong" else "الرقم السري الحالي غلط"
    val pinMustBe4 = if (english) "New PIN must be 4 digits" else "الرقم الجديد لازم يكون 4 أرقام"
    val pinMismatch = if (english) "New PIN and confirmation don't match" else "الرقم الجديد وتأكيده مش متطابقين"
    val pinChangedSuccess = if (english) "PIN changed successfully ✅" else "تم تغيير الرقم السري بنجاح ✅"
    val changePinButton = if (english) "Change PIN" else "تغيير الرقم السري"

    val lockTitle = if (english) "🔒 Lock device to this app" else "🔒 قفل الجهاز على التطبيق"
    val lockExplainer = if (english)
        "When enabled, your child can't leave the app (no Home button, no Recents) until you come back here, enter the PIN, and unlock it."
    else
        "لما تفعّل القفل، الطفل مش هيقدر يخرج من التطبيق (مفيش زرار الشاشة الرئيسية ولا التطبيقات الأخيرة) غير لما ترجع هنا وتدخل الرقم السري وتفتح القفل."
    val unlock = if (english) "🔓 Unlock" else "🔓 فتح القفل"
    val lockNow = if (english) "🔒 Lock device to BaBa Kids now" else "🔒 قفل الجهاز على BaBa Kids الآن"

    val addWordTitle = if (english) "➕ Add a new word" else "➕ إضافة كلمة جديدة"
    val wordLabel = if (english) "Word (e.g. Juice)" else "الكلمة (مثلاً: عصير)"
    val fallbackEmojiLabel = if (english) "Fallback emoji (shown if no photo)" else "إيموجي احتياطي (يظهر لو مفيش صورة)"
    val wordPictureTitle = if (english) "📷 Word picture:" else "📷 صورة الكلمة:"
    val pickFromGallery = if (english) "Pick a photo" else "اختار صورة من المعرض"
    val changePicture = if (english) "Change photo" else "غيّر الصورة"
    val clear = if (english) "Clear" else "مسح"
    val voiceTitle = if (english) "🎙️ Parent's voice for this word:" else "🎙️ صوت الوالد للكلمة:"
    val stopRecording = if (english) "⏹️ Stop recording" else "⏹️ إيقاف التسجيل"
    val recordVoice = if (english) "🎙️ Record voice" else "🎙️ تسجيل صوت"
    val preview = if (english) "▶️ Listen" else "▶️ تسميع التسجيل"
    val clearRecording = if (english) "🗑️ Clear recording" else "🗑️ مسح التسجيل"
    val micNeeded = if (english) "We need mic access to record." else "محتاجين إذن المايك عشان تسجل صوتك."
    val categoryLabel = if (english) "Category:" else "الفئة:"
    val difficultyLabel = if (english) "Difficulty level:" else "مستوى الصعوبة:"
    val wordLanguageLabel = if (english) "Show this word in:" else "الكلمة دي تظهر في:"
    val wordLanguageHint = if (english)
        "Only shows when the app language matches this choice."
    else
        "بتظهر بس لما لغة التطبيق تكون مطابقة للاختيار ده."
    val wordStarsRequiredLabel = if (english) "Stars needed to unlock (optional):" else "عدد النجوم المطلوب لفتحها (اختياري):"
    val wordStarsRequiredHint = if (english) "0 = always visible" else "0 = تظهر على طول"
    val addWordButton = if (english) "Add word" else "إضافة الكلمة"
    val addWordSuccess = if (english) "Added ✅ — check the list below." else "اتضافت ✅ — شوفها في القائمة تحت."
    val addWordFailedPrefix = if (english) "Couldn't save it: " else "معرفتش أحفظها: "

    val yourWordsTitle = if (english) "🗂️ Words you added" else "🗂️ الكلمات اللي ضفتها"
    val delete = if (english) "Delete" else "حذف"

    val footerNote = if (english)
        "Note: if you forget the PIN after changing it, there's no recovery screen in this version — keep it somewhere safe."
    else
        "ملاحظة: لو نسيت الرقم السري بعد تغييره، مفيش شاشة استرجاع في هذه النسخة — احفظه في مكان آمن."
}

/**
 * Shared design tokens for the Parent Dashboard — one place for the color
 * palette and corner-radius scale so every card/row on the dashboard reads
 * as one consistent, premium system instead of each section picking its
 * own ad-hoc colors.
 */
private object DashboardTheme {
    val primaryBlue = Color(0xFF45B7D9)
    val lightBlueBg = Color(0xFFEAF8FC)
    val purpleAccent = Color(0xFFA78BFA)
    val successGreen = Color(0xFF4CD97B)
    val warningOrange = Color(0xFFFFB84D)
    val dangerRed = Color(0xFFFF5C5C)
    val mainBackground = Color(0xFFF6F8FC)
    val cardWhite = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFF1F2937)
    val textSecondary = Color(0xFF6B7280)

    val shapeSmall = RoundedCornerShape(12.dp)
    val shapeMedium = RoundedCornerShape(18.dp)
    val shapeLarge = RoundedCornerShape(24.dp)
    val shapeExtraLarge = RoundedCornerShape(32.dp)
    val shapePill = RoundedCornerShape(50)
}

/**
 * Spec §13: a real parent dashboard — not just a locked placeholder.
 * Covers: progress overview (stars/attempts), daily usage limit, an
 * accessibility "reduce motion" toggle (§19), adding new words with a real
 * uploaded photo and a parent voice recording (§13/§14), changing the
 * parent PIN, setting the child's name and gender, app language + Arabic
 * dialect, and locking the device to this app. The dashboard itself now
 * follows the app-language setting too.
 *
 * PIN entry no longer lives here — it's ParentGateDialog above, shown as a
 * popup *before* ever navigating here, so reaching this screen at all
 * already means the PIN was verified.
 */
@Composable
fun ParentModeScreen(
    onBack: () -> Unit = {},
    deviceLocked: Boolean = false,
    onDeviceLockedChange: (Boolean) -> Unit = {},
    onManageLearnedWords: () -> Unit = {}
) {
    val context = LocalContext.current
    val parentSettings = remember { ParentSettingsManager(context) }
    val appLanguage by parentSettings.appLanguageFlow.collectAsState(
        initial = ParentSettingsManager.LANGUAGE_AR
    )
    val strings = remember(appLanguage) {
        ParentStrings(appLanguage == ParentSettingsManager.LANGUAGE_EN)
    }

    ParentDashboard(strings, onBack, deviceLocked, onDeviceLockedChange, onManageLearnedWords)
}

/**
 * The PIN/password entry for Parent Mode — now a real popup dialog shown
 * over whatever screen the parent tapped "Parent Settings" from, instead
 * of navigating to a whole new page just to ask for the PIN first. Only
 * on a correct PIN does the caller actually navigate into the dashboard.
 */
@Composable
fun ParentGateDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val parentSettings = remember { ParentSettingsManager(context) }
    val appLanguage by parentSettings.appLanguageFlow.collectAsState(
        initial = ParentSettingsManager.LANGUAGE_AR
    )
    val currentPin by parentSettings.parentPinFlow.collectAsState(
        initial = ParentSettingsManager.DEFAULT_PIN
    )
    val strings = remember(appLanguage) {
        ParentStrings(appLanguage == ParentSettingsManager.LANGUAGE_EN)
    }

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(
                    elevation = 20.dp,
                    shape = DashboardTheme.shapeLarge,
                    ambientColor = DashboardTheme.primaryBlue.copy(alpha = 0.35f),
                    spotColor = DashboardTheme.primaryBlue.copy(alpha = 0.4f)
                )
                .clip(DashboardTheme.shapeLarge)
                .background(DashboardTheme.cardWhite)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DashboardTheme.primaryBlue.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = 26.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    strings.gateTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.textPrimary
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4) pinInput = it
                        error = false
                    },
                    label = {
                        Text(strings.enterPinLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text(strings.wrongPin, color = DashboardTheme.dangerRed, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    strings.defaultPinHint,
                    fontSize = 12.sp,
                    color = DashboardTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                GlossyCard(
                    gradient = BaBaGradients.sky,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    onClick = {
                        if (pinInput.trim() == currentPin.trim()) {
                            onSuccess()
                        } else {
                            error = true
                        }
                    }
                ) {
                    Text(
                        strings.enter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (strings.english) "Cancel" else "إلغاء",
                    fontSize = 13.sp,
                    color = DashboardTheme.textSecondary,
                    modifier = Modifier.clickable { onDismiss() }.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ParentDashboard(
    strings: ParentStrings,
    onBack: () -> Unit,
    deviceLocked: Boolean,
    onDeviceLockedChange: (Boolean) -> Unit,
    onManageLearnedWords: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rewardManager = remember { RewardManager(context) }
    val parentSettings = remember { ParentSettingsManager(context) }
    val customWordsRepository = remember { CustomWordsRepository(context) }
    val customRewardsRepository = remember { CustomRewardsRepository(context) }

    val stars by rewardManager.starsFlow.collectAsState(initial = 0)
    val attempts by rewardManager.attemptsFlow.collectAsState(initial = 0)
    val streakDays by rewardManager.streakDaysFlow.collectAsState(initial = 0)
    val practicedWordsCount by rewardManager.practicedWordsCountFlow.collectAsState(initial = 0)
    val customRewards by customRewardsRepository.rewardsFlow.collectAsState(initial = emptyList())
    val dailyLimit by parentSettings.dailyLimitMinutesFlow.collectAsState(initial = 60)
    val reduceMotion by parentSettings.reduceMotionFlow.collectAsState(initial = false)
    val hapticFeedbackEnabled by parentSettings.hapticFeedbackEnabledFlow.collectAsState(initial = true)
    val wordEditModeEnabled by parentSettings.wordEditModeFlow.collectAsState(initial = false)
    val voiceEnabled by parentSettings.voiceEnabledFlow.collectAsState(initial = true)
    val voiceVolume by parentSettings.voiceVolumeFlow.collectAsState(initial = 1f)
    val autoSpeakEnabled by parentSettings.autoSpeakFlow.collectAsState(initial = true)
    var voiceCacheStatusMessage by remember { mutableStateOf<String?>(null) }
    var voiceDebugInput by remember { mutableStateOf("") }
    var generateAllProgress by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    var generateAllSummary by remember { mutableStateOf<String?>(null) }
    val customWords by customWordsRepository.customWordsFlow.collectAsState(initial = emptyList())
    val disabledCategories by parentSettings.disabledCategoriesFlow.collectAsState(initial = emptySet())
    val disabledActivityIds by parentSettings.disabledActivityIdsFlow.collectAsState(initial = emptySet())
    val pinnedActivityId by parentSettings.pinnedActivityIdFlow.collectAsState(initial = null)
    val disabledWordIds by parentSettings.disabledWordIdsFlow.collectAsState(initial = emptySet())
    val myWordsSelectedIds by parentSettings.myWordsSelectedIdsFlow.collectAsState(initial = emptySet())
    val childName by parentSettings.childNameFlow.collectAsState(initial = "")
    val childGender by parentSettings.childGenderFlow.collectAsState(
        initial = ParentSettingsManager.GENDER_MALE
    )
    val appLanguage by parentSettings.appLanguageFlow.collectAsState(
        initial = ParentSettingsManager.LANGUAGE_AR
    )
    val arabicDialect by parentSettings.arabicDialectFlow.collectAsState(
        initial = ParentSettingsManager.DIALECT_EGYPTIAN
    )
    val currentPin by parentSettings.parentPinFlow.collectAsState(
        initial = ParentSettingsManager.DEFAULT_PIN
    )

    var childNameText by remember(childName) { mutableStateOf(childName) }

    // --- PIN change form state ---
    var pinCurrentInput by remember { mutableStateOf("") }
    var pinNewInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var pinChangeMessage by remember { mutableStateOf<String?>(null) }
    var pinChangeSuccess by remember { mutableStateOf(false) }

    var dailyLimitText by remember(dailyLimit) { mutableStateOf(dailyLimit.toString()) }

    // --- Real-world reward form state ---
    var newRewardTitle by remember { mutableStateOf("") }
    var newRewardStars by remember { mutableStateOf("") }

    // --- Add-word form state ---
    var newWord by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppData.categories.first()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(1) }
    var selectedWordLanguage by remember { mutableStateOf(appLanguage) }
    var newWordStars by remember { mutableStateOf("") }
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var pendingAudioPath by remember { mutableStateOf<String?>(null) }
    // Surfaced instead of failing silently — so if the picker or the
    // recorder fails, the parent (and we, if it's reported again) sees
    // exactly what happened instead of just an empty result.
    var imagePickError by remember { mutableStateOf(false) }
    var recordingError by remember { mutableStateOf(false) }
    // Diagnostic for the add-word flow specifically — after repeated
    // reports of "nothing happens" that couldn't be reproduced or
    // explained by code review alone, this makes every outcome (success
    // or the exact exception) visible on screen instead of guessing again.
    var addWordStatus by remember { mutableStateOf<String?>(null) }
    var addWordStatusIsError by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = MediaStorage.copyImage(context, uri)
            pendingImagePath = path
            imagePickError = path == null
        }
    }

    // --- Voice recording state ---
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var micPermissionExplainer by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaRecorder?.stop() }
            mediaRecorder?.release()
            previewPlayer?.release()
        }
    }

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun startRecording() {
        recordingError = false
        val path = MediaStorage.newAudioFilePath(context)
        val result = runCatching {
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // Higher quality than the encoder's defaults — a clearer
                // recording for the child to hear back.
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(path)
                prepare()
                start()
            }
            mediaRecorder = recorder
            pendingAudioPath = path
            isRecording = true
        }
        if (result.isFailure) recordingError = true
    }

    fun stopRecording() {
        val result = runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }
        if (result.isFailure) {
            // stop() throws when too little audio was captured; the
            // resulting file is invalid, so don't keep a reference to it.
            MediaStorage.deleteIfExists(pendingAudioPath)
            pendingAudioPath = null
        }
        mediaRecorder = null
        isRecording = false
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionExplainer = false
            startRecording()
        } else {
            micPermissionExplainer = true
        }
    }

    // Merged settings list: null = main list, otherwise the key of the
    // one section currently shown as its own full page.
    var activeSection by remember { mutableStateOf<String?>(null) }

    // Content-control drill-down: null = normal dashboard, "categories" =
    // full-page category list, any other value = the word list for that
    // one category. A real separate screen for each level (not an inline
    // accordion in the middle of a long settings scroll), done as internal
    // state instead of new navigation routes — far lower risk of breaking
    // the app's existing navigation graph than wiring brand-new routes for
    // something this deep in an already-long file.
    var contentControlPage by remember { mutableStateOf<String?>(null) }

    // BUG FIX: without this, the phone's own back button/gesture bypassed
    // all of the state above entirely — it's handled by the NavController
    // popping this whole screen off the back stack, which has no idea
    // "activeSection" or "contentControlPage" exist. Tapping the on-screen
    // back arrow worked (it's wired to update that state directly), but
    // the *system* back button always exited Parent Mode completely, no
    // matter how deep in a sub-page you were. This intercepts the system
    // back action first and, if we're inside any sub-page, steps back one
    // level in our own state instead of letting the NavController see it
    // at all — only once both are back at null does system back resume
    // its normal behavior (actually leaving Parent Mode).
    androidx.activity.compose.BackHandler(enabled = activeSection != null || contentControlPage != null) {
        when {
            contentControlPage != null && contentControlPage != "categories" -> contentControlPage = "categories"
            contentControlPage == "categories" -> contentControlPage = null
            activeSection != null -> activeSection = null
        }
    }

    if (contentControlPage == "categories") {
        ContentControlCategoriesPage(
            strings = strings,
            disabledCategories = disabledCategories,
            onToggleCategory = { id, enabled -> scope.launch { parentSettings.setCategoryEnabled(id, enabled) } },
            onOpenCategory = { categoryId -> contentControlPage = categoryId },
            onBack = { contentControlPage = null }
        )
        return
    }
    if (contentControlPage != null) {
        ContentControlWordsPage(
            categoryId = contentControlPage!!,
            strings = strings,
            customWords = customWords,
            disabledWordIds = disabledWordIds,
            myWordsSelectedIds = myWordsSelectedIds,
            onToggleWord = { id, enabled -> scope.launch { parentSettings.setWordEnabled(id, enabled) } },
            onToggleMyWords = { id, inMyWords ->
                scope.launch { parentSettings.setWordInMyWords(id, inMyWords) }
            },
            onBack = { contentControlPage = "categories" }
        )
        return
    }

    if (activeSection == "languageTitle") {
        SettingsSubPage(title = strings.languageTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.languageTitle, accent = DashboardTheme.primaryBlue) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = appLanguage == ParentSettingsManager.LANGUAGE_AR,
                        onClick = {
                            scope.launch { parentSettings.setAppLanguage(ParentSettingsManager.LANGUAGE_AR) }
                        },
                        label = { Text(strings.arabic) }
                    )
                    FilterChip(
                        selected = appLanguage == ParentSettingsManager.LANGUAGE_EN,
                        onClick = {
                            scope.launch { parentSettings.setAppLanguage(ParentSettingsManager.LANGUAGE_EN) }
                        },
                        label = { Text(strings.english_) }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(strings.languageHint, fontSize = 11.sp, color = Color.Gray)

                if (appLanguage == ParentSettingsManager.LANGUAGE_AR) {
                    Spacer(Modifier.height(14.dp))
                    Text(strings.dialectTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = arabicDialect == ParentSettingsManager.DIALECT_EGYPTIAN,
                            onClick = {
                                scope.launch {
                                    parentSettings.setArabicDialect(ParentSettingsManager.DIALECT_EGYPTIAN)
                                }
                            },
                            label = { Text(strings.dialectEgyptian) }
                        )
                        FilterChip(
                            selected = arabicDialect == ParentSettingsManager.DIALECT_FUSHA,
                            onClick = {
                                scope.launch {
                                    parentSettings.setArabicDialect(ParentSettingsManager.DIALECT_FUSHA)
                                }
                            },
                            label = { Text(strings.dialectFusha) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(strings.dialectHint, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        return
    }
    if (activeSection == "dailyLimitTitle") {
        SettingsSubPage(title = strings.dailyLimitTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.dailyLimitTitle, accent = DashboardTheme.warningOrange) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        value = dailyLimitText,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                dailyLimitText = value
                            }
                        },
                        label = {
                            Text(strings.minutesPerDay, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val minutes = dailyLimitText.toIntOrNull() ?: dailyLimit
                            scope.launch { parentSettings.setDailyLimitMinutes(minutes) }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(strings.save)
                    }
                }
                Text(strings.zeroForNoLimit, fontSize = 12.sp, color = Color.Gray)
            }
        }
        return
    }
    if (activeSection == "accessibilityTitle") {
        SettingsSubPage(title = strings.accessibilityTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.accessibilityTitle) {
                SettingsToggleRow(
                    label = strings.reduceMotion,
                    checked = reduceMotion,
                    onCheckedChange = { checked -> scope.launch { parentSettings.setReduceMotion(checked) } }
                )
                SettingsToggleRow(
                    label = strings.hapticFeedbackLabel,
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { checked -> scope.launch { parentSettings.setHapticFeedbackEnabled(checked) } }
                )
                SettingsToggleRow(
                    label = strings.wordEditModeLabel,
                    checked = wordEditModeEnabled,
                    onCheckedChange = { checked -> scope.launch { parentSettings.setWordEditModeEnabled(checked) } }
                )
                SettingsToggleRow(
                    label = strings.voiceEnabledLabel,
                    checked = voiceEnabled,
                    onCheckedChange = { checked -> scope.launch { parentSettings.setVoiceEnabled(checked) } }
                )
                SettingsToggleRow(
                    label = strings.autoSpeakLabel,
                    checked = autoSpeakEnabled,
                    onCheckedChange = { checked -> scope.launch { parentSettings.setAutoSpeak(checked) } },
                    showDivider = false
                )
                Spacer(Modifier.height(10.dp))
                Text(strings.voiceVolumeLabel, fontSize = 15.sp)
                androidx.compose.material3.Slider(
                    value = voiceVolume,
                    onValueChange = { newVolume ->
                        scope.launch { parentSettings.setVoiceVolume(newVolume) }
                    },
                    valueRange = 0f..1f
                )
                Spacer(Modifier.height(14.dp))
                Text(strings.clearVoiceCacheHint, fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        com.babakids.app.audio.AudioCacheManager(context).clearAll()
                        voiceCacheStatusMessage = strings.voiceCacheClearedMessage
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.clearVoiceCacheButton)
                }
                voiceCacheStatusMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
            }
        }
        return
    }
    if (activeSection == "voiceDiagnosticsTitle") {
        SettingsSubPage(title = strings.voiceDiagnosticsTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.voiceDiagnosticsTitle, accent = DashboardTheme.successGreen) {
                Text(strings.voiceDiagnosticsHint, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                Text(
                    strings.bundledAudioCountLabel + " " +
                        com.babakids.app.audio.SmartVoiceManager.getInstance(context).bundledEntryCount(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = voiceDebugInput,
                    onValueChange = { voiceDebugInput = it },
                    label = {
                        Text(
                            strings.voiceDebugInputLabel,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val textToTest = voiceDebugInput.trim().ifBlank {
                            if (strings.english) "This is a voice test" else "ده اختبار الصوت"
                        }
                        val tester = com.babakids.app.audio.SmartVoiceManager.getInstance(context)
                        tester.playSmartVoice(textToTest, english = strings.english)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.voiceTestButton)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    com.babakids.app.audio.VoiceDiagnostics.lastAttempt ?: strings.voiceDiagnosticsNone,
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Divider(modifier = Modifier.padding(vertical = 14.dp))
                Text(strings.generateAllTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(strings.generateAllHint, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                if (generateAllProgress != null) {
                    val (current, total, currentText) = generateAllProgress!!
                    LinearProgressIndicator(
                        progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("$current / $total — \"$currentText\"", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                val generator = com.babakids.app.audio.OfflineAudioPreGenerator(context)
                                val summary = generator.generateAll { progress ->
                                    generateAllProgress = Triple(progress.current, progress.total, progress.text)
                                }
                                generateAllProgress = null
                                generateAllSummary = if (strings.english) {
                                    "Done — generated ${summary.generated}, already cached ${summary.skippedAlreadyCached}, failed ${summary.failed} (of ${summary.total})"
                                } else {
                                    "خلصت — اتولّد ${summary.generated}، كان محفوظ من الأول ${summary.skippedAlreadyCached}، فشل ${summary.failed} (من أصل ${summary.total})"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.generateAllButton)
                    }
                }
                generateAllSummary?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 13.sp, color = Color(0xFF2E7D32))
                }
            }
        }
        return
    }
    if (activeSection == "rewardsTitle") {
        SettingsSubPage(title = strings.rewardsTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.rewardsTitle, accent = DashboardTheme.warningOrange) {
                Text(strings.rewardsHint, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = newRewardTitle,
                    onValueChange = { newRewardTitle = it },
                    label = {
                        Text(strings.rewardTitleLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = newRewardStars,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 3) newRewardStars = value
                    },
                    label = {
                        Text(strings.rewardStarsLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val title = newRewardTitle.trim()
                        val starsNeeded = newRewardStars.toIntOrNull()
                        if (title.isNotEmpty() && starsNeeded != null && starsNeeded > 0) {
                            scope.launch {
                                customRewardsRepository.addReward(title, starsNeeded)
                                newRewardTitle = ""
                                newRewardStars = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.addRewardButton)
                }

                if (customRewards.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(strings.yourRewardsTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    customRewards.forEach { reward ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${reward.title} (${reward.starsRequired} ⭐)", fontSize = 14.sp)
                                reward.earnedAt?.let { timestamp ->
                                    Text(
                                        strings.achievedOnLabel + formatRewardTimestamp(timestamp),
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            OutlinedButton(onClick = {
                                scope.launch { customRewardsRepository.removeReward(reward.id) }
                            }) {
                                Text(strings.delete)
                            }
                        }
                        Divider()
                    }
                }
            }
        }
        return
    }
    if (activeSection == "childNameTitle") {
        SettingsSubPage(title = strings.childNameTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.childNameTitle, accent = DashboardTheme.purpleAccent) {
                Text(strings.childNameHint, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        value = childNameText,
                        onValueChange = { childNameText = it },
                        label = {
                            Text(strings.childNameLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { scope.launch { parentSettings.setChildName(childNameText) } },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text(strings.save) }
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.childGenderLabel, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = childGender == ParentSettingsManager.GENDER_MALE,
                        onClick = {
                            scope.launch { parentSettings.setChildGender(ParentSettingsManager.GENDER_MALE) }
                        },
                        label = { Text(strings.boy) }
                    )
                    FilterChip(
                        selected = childGender == ParentSettingsManager.GENDER_FEMALE,
                        onClick = {
                            scope.launch { parentSettings.setChildGender(ParentSettingsManager.GENDER_FEMALE) }
                        },
                        label = { Text(strings.girl) }
                    )
                }
                Text(strings.genderHint, fontSize = 11.sp, color = Color.Gray)
            }
        }
        return
    }
    if (activeSection == "progressTitle") {
        SettingsSubPage(title = strings.progressTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.progressTitle, accent = DashboardTheme.purpleAccent) {
                Text(strings.totalStars + stars, fontSize = 16.sp)
                Text(strings.totalAttempts + attempts, fontSize = 16.sp)
                Text(strings.streakLabel + streakDays, fontSize = 16.sp)
                Text(strings.practicedLabel + practicedWordsCount, fontSize = 16.sp)
                Text(
                    strings.wordsAvailable + (AppData.words.size + customWords.size) +
                        " (${strings.youAdded}${customWords.size})",
                    fontSize = 16.sp
                )
            }
        }
        return
    }
    if (activeSection == "yourWordsTitle") {
        SettingsSubPage(title = strings.yourWordsTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.yourWordsTitle, accent = DashboardTheme.successGreen) {
                if (customWords.isEmpty()) {
                    Text(
                        if (strings.english) "No custom words yet." else "لسه مفيش كلمات مضافة.",
                        fontSize = 13.sp,
                        color = DashboardTheme.textSecondary
                    )
                }
                customWords.forEach { word ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${word.emoji} ${word.word}", fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        OutlinedButton(onClick = {
                            scope.launch { customWordsRepository.removeWord(word.id) }
                        }) {
                            Text(strings.delete)
                        }
                    }
                    Divider()
                }
            }
        }
        return
    }
    if (activeSection == "gamesTitle") {
        SettingsSubPage(
            title = if (strings.english) "🎮 Games" else "🎮 الألعاب",
            onBack = { activeSection = null }
        ) {
            SectionCard(
                title = if (strings.english) "🎮 Games" else "🎮 الألعاب",
                accent = DashboardTheme.purpleAccent
            ) {
                Text(
                    if (strings.english) "Show, hide, or pin a favorite game."
                    else "اعرض، اخفِ، أو ثبّت لعبة مفضلة.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(10.dp))
                allGameActivities.forEach { game ->
                    val enabled = game.id !in disabledActivityIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("${game.emoji} ${if (strings.english) game.titleEn else game.titleAr}", fontSize = 15.sp)
                        }
                        Text(
                            if (game.id == pinnedActivityId) "📌" else "📍",
                            fontSize = 16.sp,
                            color = if (game.id == pinnedActivityId) Color.Unspecified else Color.LightGray,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        parentSettings.setPinnedActivity(if (game.id == pinnedActivityId) null else game.id)
                                    }
                                }
                                .padding(horizontal = 10.dp)
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                scope.launch { parentSettings.setActivityEnabled(game.id, checked) }
                            }
                        )
                    }
                    Divider()
                }
            }
        }
        return
    }
    if (activeSection == "changePinTitle") {
        SettingsSubPage(title = strings.changePinTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.changePinTitle, accent = DashboardTheme.dangerRed) {
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = pinCurrentInput,
                    onValueChange = { if (it.length <= 4) pinCurrentInput = it },
                    label = {
                        Text(strings.currentPinLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = pinNewInput,
                    onValueChange = { if (it.length <= 4) pinNewInput = it },
                    label = {
                        Text(strings.newPinLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = pinConfirmInput,
                    onValueChange = { if (it.length <= 4) pinConfirmInput = it },
                    label = {
                        Text(strings.confirmPinLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                pinChangeMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        fontSize = 13.sp,
                        color = if (pinChangeSuccess) Color(0xFF2E7D32) else Color.Red
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        when {
                            pinCurrentInput.trim() != currentPin.trim() -> {
                                pinChangeSuccess = false
                                pinChangeMessage = strings.pinWrongCurrent
                            }
                            pinNewInput.length != 4 -> {
                                pinChangeSuccess = false
                                pinChangeMessage = strings.pinMustBe4
                            }
                            pinNewInput != pinConfirmInput -> {
                                pinChangeSuccess = false
                                pinChangeMessage = strings.pinMismatch
                            }
                            else -> {
                                // Wait for the write to actually complete before
                                // reporting success — showing "success" before the
                                // DataStore write finishes meant a quick app-close
                                // right after could lose the change.
                                scope.launch {
                                    parentSettings.setParentPin(pinNewInput)
                                    pinChangeSuccess = true
                                    pinChangeMessage = strings.pinChangedSuccess
                                    pinCurrentInput = ""
                                    pinNewInput = ""
                                    pinConfirmInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.changePinButton)
                }
            }
        }
        return
    }
    if (activeSection == "addWordTitle") {
        SettingsSubPage(title = strings.addWordTitle, onBack = { activeSection = null }) {
            SectionCard(title = strings.addWordTitle, accent = DashboardTheme.successGreen) {
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = {
                        Text(strings.wordLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = newEmoji,
                    onValueChange = { newEmoji = it },
                    label = {
                        Text(strings.fallbackEmojiLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text(strings.wordPictureTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pickedBitmap = remember(pendingImagePath) {
                        pendingImagePath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                    }
                    if (pickedBitmap != null) {
                        Image(
                            bitmap = pickedBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 12.dp)
                        )
                    }
                    OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(if (pendingImagePath == null) strings.pickFromGallery else strings.changePicture)
                    }
                    if (pendingImagePath != null) {
                        OutlinedButton(
                            onClick = { pendingImagePath = null },
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text(strings.clear) }
                    }
                }
                if (imagePickError) {
                    Text(strings.imagePickErrorMsg, fontSize = 12.sp, color = Color(0xFFB71C1C))
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.voiceTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else if (hasMicPermission()) {
                            startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFE53935) else ButtonDefaults.buttonColors().containerColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRecording) strings.stopRecording else strings.recordVoice)
                }
                if (pendingAudioPath != null && !isRecording) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    previewPlayer?.release()
                                    val player = MediaPlayer().apply {
                                        setDataSource(pendingAudioPath)
                                        setOnCompletionListener { it.release() }
                                        prepare()
                                        start()
                                    }
                                    previewPlayer = player
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.preview) }
                        OutlinedButton(
                            onClick = { pendingAudioPath = null },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.clearRecording) }
                    }
                }
                if (micPermissionExplainer) {
                    Text(strings.micNeeded, fontSize = 12.sp, color = Color(0xFFB71C1C))
                }
                if (recordingError) {
                    Text(strings.recordingErrorMsg, fontSize = 12.sp, color = Color(0xFFB71C1C))
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.categoryLabel, fontSize = 14.sp)
                Box {
                    OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                        Text("${selectedCategory.emoji} ${selectedCategory.displayTitle(strings.english)}")
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        AppData.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.displayTitle(strings.english)}") },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(strings.difficultyLabel, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { level ->
                        FilterChip(
                            selected = selectedDifficulty == level,
                            onClick = { selectedDifficulty = level },
                            label = { Text(level.toString()) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(strings.wordLanguageLabel, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedWordLanguage == ParentSettingsManager.LANGUAGE_AR,
                        onClick = { selectedWordLanguage = ParentSettingsManager.LANGUAGE_AR },
                        label = { Text(strings.arabic) }
                    )
                    FilterChip(
                        selected = selectedWordLanguage == ParentSettingsManager.LANGUAGE_EN,
                        onClick = { selectedWordLanguage = ParentSettingsManager.LANGUAGE_EN },
                        label = { Text(strings.english_) }
                    )
                }
                Text(strings.wordLanguageHint, fontSize = 11.sp, color = Color.Gray)

                Spacer(Modifier.height(12.dp))
                Text(strings.wordStarsRequiredLabel, fontSize = 14.sp)
                OutlinedTextField(
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    value = newWordStars,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 4) newWordStars = value
                    },
                    label = {
                        Text(
                            strings.wordStarsRequiredHint,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val wordToAdd = newWord.trim()
                        if (wordToAdd.isNotEmpty()) {
                            addWordStatus = null
                            val emojiToAdd = newEmoji
                            val categoryToAdd = selectedCategory.id
                            val difficultyToAdd = selectedDifficulty
                            val imageToAdd = pendingImagePath
                            val audioToAdd = pendingAudioPath
                            val languageToAdd = selectedWordLanguage
                            val starsToAdd = newWordStars.toIntOrNull() ?: 0
                            scope.launch {
                                try {
                                    customWordsRepository.addWord(
                                        word = wordToAdd,
                                        emoji = emojiToAdd.ifBlank { "🔤" },
                                        category = categoryToAdd,
                                        difficulty = difficultyToAdd,
                                        imagePath = imageToAdd,
                                        audioPath = audioToAdd,
                                        wordLanguage = languageToAdd,
                                        starsRequired = starsToAdd
                                    )
                                    // Only clear the form once the write has
                                    // genuinely succeeded — clearing it
                                    // beforehand meant a failed/slow save
                                    // silently lost whatever was typed.
                                    newWord = ""
                                    newEmoji = ""
                                    newWordStars = ""
                                    pendingImagePath = null
                                    pendingAudioPath = null
                                    addWordStatusIsError = false
                                    addWordStatus = strings.addWordSuccess
                                } catch (e: Exception) {
                                    addWordStatusIsError = true
                                    addWordStatus = strings.addWordFailedPrefix + (e.message ?: e.toString())
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.addWordButton)
                }
                addWordStatus?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        fontSize = 13.sp,
                        color = if (addWordStatusIsError) Color.Red else Color(0xFF2E7D32)
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardTheme.mainBackground)
    ) {
    BackTopBar(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                strings.dashboardTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        item { ParentProfileCard(childName = childName, english = strings.english) }

        item {
            ChildStatisticsCard(
                stars = stars,
                streakDays = streakDays,
                achievementCount = com.babakids.app.data.StickerCollection.unlockedFor(stars).size,
                english = strings.english
            )
        }

        // --- Developer info — always visible, top of the list, both tabs ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2F3A))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👨‍💻", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            strings.developerLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B3C0)
                        )
                        Text(
                            "Mahmoud.mady30@gmail.com",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- App lock (kiosk-style) — kept right under the developer card, per request ---
        item {
            AppLockSection(strings, deviceLocked, onDeviceLockedChange)
        }

        // Merged: everyday and advanced settings now live in one unified,
        // organized list — no more tab switch. Each row below opens its
        // own dedicated page instead of expanding inline.

        // ============================================================
        // Categorized like any standard settings screen: general things
        // first and grouped together, then content, then progress, then
        // parental controls, then advanced/technical tools last.
        // ============================================================

        // --- عام / General ---
        item {
            Text(
                if (strings.english) "GENERAL" else "عام",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
        item {
            SettingsNavRow(title = strings.languageTitle, accent = DashboardTheme.primaryBlue) { activeSection = "languageTitle" }
        }
        item {
            SettingsNavRow(title = strings.accessibilityTitle) { activeSection = "accessibilityTitle" }
        }

        // --- الطفل والمحتوى / Child & Content ---
        item {
            Text(
                if (strings.english) "CHILD & CONTENT" else "الطفل والمحتوى",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
        }
        item {
            SettingsNavRow(title = strings.childNameTitle, accent = DashboardTheme.purpleAccent) { activeSection = "childNameTitle" }
        }
        item {
            SettingsNavRow(title = strings.addWordTitle, accent = DashboardTheme.successGreen) { activeSection = "addWordTitle" }
        }
        if (customWords.isNotEmpty()) {
            item {
                SettingsNavRow(title = strings.yourWordsTitle, accent = DashboardTheme.successGreen) { activeSection = "yourWordsTitle" }
            }
        }
        item {
            SettingsNavRow(title = strings.contentControlTitle, accent = DashboardTheme.successGreen) { contentControlPage = "categories" }
        }
        item {
            SettingsNavRow(
                title = if (strings.english) "🎮 Games" else "🎮 الألعاب",
                accent = DashboardTheme.purpleAccent
            ) { activeSection = "gamesTitle" }
        }

        // --- التقدم والمكافآت / Progress & Rewards ---
        item {
            Text(
                if (strings.english) "PROGRESS & REWARDS" else "التقدم والمكافآت",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
        }
        item {
            SettingsNavRow(title = strings.progressTitle, accent = DashboardTheme.purpleAccent) { activeSection = "progressTitle" }
        }
        item {
            SettingsNavRow(
                title = if (strings.english) "📖 Learned Words" else "📖 الكلمات المتعلمة",
                accent = DashboardTheme.successGreen
            ) { onManageLearnedWords() }
        }
        item {
            SettingsNavRow(title = strings.rewardsTitle, accent = DashboardTheme.warningOrange) { activeSection = "rewardsTitle" }
        }

        // --- الرقابة الأبوية / Parental Controls ---
        item {
            Text(
                if (strings.english) "PARENTAL CONTROLS" else "الرقابة الأبوية",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
        }
        item {
            SettingsNavRow(title = strings.dailyLimitTitle, accent = DashboardTheme.warningOrange) { activeSection = "dailyLimitTitle" }
        }
        item {
            SettingsNavRow(title = strings.changePinTitle, accent = DashboardTheme.dangerRed) { activeSection = "changePinTitle" }
        }

        // --- متقدم / Advanced ---
        item {
            Text(
                if (strings.english) "ADVANCED" else "متقدم",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
        }
        // Voice debug screen: input text, generate/play, cache status —
        // matches the spec's debug-screen request, built into Parent Mode
        // rather than a separate hidden screen. Shows exactly what
        // happened on the last attempt (bundled hit / cache hit /
        // fallback + why) so "the voice isn't working" reports have a
        // real, visible answer instead of another guess. A technical/
        // debug tool, not something a parent needs day-to-day — kept last.
        item {
            SettingsNavRow(title = strings.voiceDiagnosticsTitle, accent = DashboardTheme.successGreen) { activeSection = "voiceDiagnosticsTitle" }
        }

        item {
            Text(strings.footerNote, fontSize = 12.sp, color = Color.Gray)
        }
    }
    }
}

/**
 * Locks the device to this app using Android's Lock Task Mode
 * (startLockTask/stopLockTask). Without the app being provisioned as
 * Device Owner (which needs ADB/MDM setup, not something a normal install
 * can do), Android runs this as standard "Screen Pinning": Home and
 * Recents are blocked while active, and the system may show a one-time
 * confirmation the first time. Exiting still goes through this same
 * screen (PIN-protected), which is what satisfies "press a button, then
 * type a password" from the request.
 */
@Composable
private fun AppLockSection(
    strings: ParentStrings,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    SectionCard(title = strings.lockTitle, accent = DashboardTheme.dangerRed) {
        Text(strings.lockExplainer, fontSize = 13.sp, color = DashboardTheme.textSecondary)
        Spacer(Modifier.height(10.dp))
        GlossyCard(
            gradient = if (locked) BaBaGradients.orange else BaBaGradients.sky,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                if (locked) {
                    runCatching { activity?.stopLockTask() }
                    onLockedChange(false)
                } else {
                    runCatching { activity?.startLockTask() }
                    onLockedChange(true)
                }
            }
        ) {
            Text(
                if (locked) strings.unlock else strings.lockNow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Top-of-dashboard "welcome" card. No parent name/avatar system exists in
 * this app (no login, no profile photo upload) so this greets generically
 * rather than inventing one — the personalization that IS real is the
 * child's own name, which the subtitle uses.
 */
@Composable
private fun ParentProfileCard(childName: String, english: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = DashboardTheme.shapeLarge,
                ambientColor = DashboardTheme.primaryBlue.copy(alpha = 0.3f),
                spotColor = DashboardTheme.primaryBlue.copy(alpha = 0.35f)
            )
            .clip(DashboardTheme.shapeLarge)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(DashboardTheme.primaryBlue, Color(0xFF3AA0C4))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👋", fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (english) "Welcome!" else "أهلاً بيك 👋",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (childName.isNotBlank()) {
                        if (english) "Managing ${childName}'s experience in BaBa"
                        else "إدارة تجربة $childName في BaBa"
                    } else {
                        if (english) "Managing your child's experience in BaBa"
                        else "إدارة تجربة طفلك في BaBa"
                    },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/** Real stats only — stars, streak, and unlocked stickers. No fabricated "level" or "games played" counters exist in the data model. */
@Composable
private fun ChildStatisticsCard(stars: Int, streakDays: Int, achievementCount: Int, english: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatMiniCard(
            emoji = "⭐",
            value = stars.toString(),
            label = if (english) "Stars" else "نجمة",
            accent = DashboardTheme.warningOrange,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            emoji = "🔥",
            value = streakDays.toString(),
            label = if (english) "Day streak" else "أيام متتالية",
            accent = DashboardTheme.dangerRed,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            emoji = "🏆",
            value = achievementCount.toString(),
            label = if (english) "Badges" else "شارات",
            accent = DashboardTheme.purpleAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMiniCard(emoji: String, value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = DashboardTheme.shapeMedium, ambientColor = accent.copy(alpha = 0.2f))
            .clip(DashboardTheme.shapeMedium)
            .background(DashboardTheme.cardWhite)
            .padding(vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DashboardTheme.textPrimary)
            Text(label, fontSize = 11.sp, color = DashboardTheme.textSecondary)
        }
    }
}

/**
 * Every settings section uses this — a single visual upgrade here lifts
 * the whole screen at once instead of hand-tweaking a dozen call sites,
 * which also means far less risk of breaking a working screen than
 * reshuffling 1000+ lines of interleaved state and content by hand.
 *
 * The "3D" feel is a soft two-layer shadow (a faint colored glow behind a
 * tighter dark shadow) plus a subtle vertical gradient on the card face —
 * flat cards read as flat, a hint of light-from-above depth reads as a
 * tactile, tappable object. The leading emoji already present in most
 * section titles becomes a small colored badge instead of sitting inline

 * with plain gray text, which is what actually made the settings screen
 * look like a flat list of gray labels instead of organized sections.
 */
/**
 * The compact, tappable row used in the unified settings list — replaces
 * what used to be a full SectionCard inline. Tapping opens that setting's
 * own dedicated page via SettingsSubPage below, instead of expanding
 * inline in the middle of a long scroll.
 */
@Composable
private fun SettingsNavRow(title: String, subtitle: String? = null, accent: Color = DashboardTheme.primaryBlue, onClick: () -> Unit) {
    val firstChar = title.codePointAt(0)
    val hasLeadingEmoji = firstChar > 0x2000
    val badge = if (hasLeadingEmoji) title.substring(0, Character.charCount(firstChar)) else "•"
    val labelText = if (hasLeadingEmoji) title.substring(Character.charCount(firstChar)).trim() else title
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = DashboardTheme.shapeMedium, ambientColor = accent.copy(alpha = 0.18f))
            .clip(DashboardTheme.shapeMedium)
            .background(DashboardTheme.cardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(labelText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DashboardTheme.textPrimary)
            subtitle?.let {
                Text(it, fontSize = 12.sp, color = DashboardTheme.textSecondary)
            }
        }
        Text("›", fontSize = 20.sp, color = DashboardTheme.textSecondary)
    }
}

/**
 * A properly-behaved toggle row for use inside any settings page's
 * SectionCard: the label always wraps and never pushes the switch off
 * the edge of the card (the bug that made "Word Edit Mode" overflow —
 * its Text had no weight(1f), so a long label just kept growing instead
 * of wrapping). The whole row is tappable (not just the switch itself),
 * which is both a bigger, easier touch target and what gives the
 * "professional" tactile feel — with a soft press ripple confined to a
 * rounded rect instead of the row's tap bleeding square-edged into its
 * neighbors. Switch itself is set non-interactive (onCheckedChange =
 * null) so tapping it doesn't ALSO fire the row's own click handler and
 * double-toggle the value.
 *
 * showDivider draws a thin rule below the row — set false only on the
 * last item in a group, so a card of toggles reads as a single grouped
 * list (à la iOS/Android system settings) instead of loose floating rows.
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Text(
                label,
                fontSize = 15.sp,
                color = DashboardTheme.textPrimary,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Switch(checked = checked, onCheckedChange = null)
        }
        if (showDivider) {
            androidx.compose.material3.Divider(
                color = DashboardTheme.mainBackground,
                thickness = 1.5.dp
            )
        }
    }
}

/** Full-page shell every converted settings section renders inside — a consistent header + back button + scrollable body. */
@Composable
private fun SettingsSubPage(title: String, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(DashboardTheme.mainBackground)) {
        BackTopBar(onBack = onBack)
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardTheme.textPrimary,
            modifier = Modifier.padding(16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Full-page list of categories (with an enable/disable switch each) — this
 * is what used to be an inline accordion buried in the middle of the
 * settings scroll. Tapping a category's name opens ContentControlWordsPage
 * for just that one category's words.
 */
@Composable
private fun ContentControlCategoriesPage(
    strings: ParentStrings,
    disabledCategories: Set<String>,
    onToggleCategory: (String, Boolean) -> Unit,
    onOpenCategory: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(DashboardTheme.mainBackground)) {
        BackTopBar(onBack = onBack)
        Text(
            if (strings.english) "Sections" else "الأقسام",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardTheme.textPrimary,
            modifier = Modifier.padding(16.dp)
        )
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AppData.categories) { category ->
                val enabled = category.id !in disabledCategories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = DashboardTheme.shapeMedium, ambientColor = DashboardTheme.primaryBlue.copy(alpha = 0.15f))
                        .clip(DashboardTheme.shapeMedium)
                        .background(DashboardTheme.cardWhite)
                        .clickable { onOpenCategory(category.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(category.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            category.displayTitle(strings.english),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DashboardTheme.textPrimary
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked -> onToggleCategory(category.id, checked) }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (strings.english) "›" else "‹", fontSize = 18.sp, color = DashboardTheme.textSecondary)
                }
            }
        }
    }
}

/** Full-page word list for exactly one category — enable/disable each word, and mark/unmark it for "My Words". */
@Composable
private fun ContentControlWordsPage(
    categoryId: String,
    strings: ParentStrings,
    customWords: List<com.babakids.app.data.WordItem>,
    disabledWordIds: Set<String>,
    myWordsSelectedIds: Set<String>,
    onToggleWord: (String, Boolean) -> Unit,
    onToggleMyWords: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val category = AppData.categoryFor(categoryId)
    val categoryWords = if (categoryId == "my_words") {
        customWords
    } else {
        AppData.wordsFor(categoryId, strings.english) + customWords.filter { it.category == categoryId }
    }

    Column(modifier = Modifier.fillMaxSize().background(DashboardTheme.mainBackground)) {
        BackTopBar(onBack = onBack)
        Text(
            "${category.emoji} ${category.displayTitle(strings.english)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardTheme.textPrimary,
            modifier = Modifier.padding(16.dp)
        )
        if (categoryWords.isEmpty()) {
            Text(
                if (strings.english) "No words here yet." else "لسه مفيش كلمات هنا.",
                fontSize = 14.sp,
                color = DashboardTheme.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categoryWords, key = { it.id }) { word ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = DashboardTheme.shapeMedium, ambientColor = DashboardTheme.successGreen.copy(alpha = 0.15f))
                        .clip(DashboardTheme.shapeMedium)
                        .background(DashboardTheme.cardWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(word.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(word.displayWord(strings.english), fontSize = 15.sp, color = DashboardTheme.textPrimary)
                    }
                    if (categoryId != "my_words") {
                        Text(
                            text = if (word.id in myWordsSelectedIds) "❤️" else "🤍",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { onToggleMyWords(word.id, word.id !in myWordsSelectedIds) }
                                .padding(horizontal = 8.dp)
                        )
                    }
                    Switch(
                        checked = word.id !in disabledWordIds,
                        onCheckedChange = { checked -> onToggleWord(word.id, checked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, accent: Color = DashboardTheme.primaryBlue, content: @Composable () -> Unit) {
    // Split a leading emoji (if any) from the rest of the title so it can
    // become a badge — most section titles already start with one.
    val firstChar = title.codePointAt(0)
    val hasLeadingEmoji = firstChar > 0x2000 // rough emoji/symbol range check
    val badge = if (hasLeadingEmoji) title.substring(0, Character.charCount(firstChar)) else "•"
    val labelText = if (hasLeadingEmoji) title.substring(Character.charCount(firstChar)).trim() else title

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Faint colored glow, offset slightly down — the "light source
            // from above" cue that reads as gentle depth rather than a flat
            // sticker, without looking heavy or cluttered.
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = accent.copy(alpha = 0.25f),
                spotColor = accent.copy(alpha = 0.35f)
            )
    ) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(22.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFFCFCFF))
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badge, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        labelText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A3A3C),
                        letterSpacing = 0.2.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}
