/**
 * Copyright (C) 2014 wak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.wakhub.monodict.activity.bean;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.github.wakhub.monodict.R;
import com.github.wakhub.monodict.preferences.Preferences_;
import com.github.wakhub.monodict.utils.DateTimeUtils;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by wak on 5/26/14.
 * <p/>
 * memo:
 * - UtteranceProgressListener is available since api 15
 * <p/>
 */
@EBean
public class SpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = SpeechHelper.class.getSimpleName();

    private static final Locale THAI_LOCALE = new Locale("th");

    public static final int REQUEST_CODE_INIT_ENGLISH_ENGINE = 20300;
    public static final int REQUEST_CODE_INIT_FRENCH_ENGINE = 20301;
    public static final int REQUEST_CODE_INIT_JAPANESE_ENGINE = 20302;
    public static final int REQUEST_CODE_INIT_CHINESE_ENGINE = 20303;
    public static final int REQUEST_CODE_INIT_KOREAN_ENGINE = 20304;
    public static final int REQUEST_CODE_INIT_THAI_ENGINE = 20305;

    public static final Integer[] REQUEST_CODE_LIST_OF_INIT_ENGINE = new Integer[]{
            REQUEST_CODE_INIT_ENGLISH_ENGINE,
            REQUEST_CODE_INIT_FRENCH_ENGINE,
            REQUEST_CODE_INIT_JAPANESE_ENGINE,
            REQUEST_CODE_INIT_CHINESE_ENGINE,
            REQUEST_CODE_INIT_KOREAN_ENGINE,
            REQUEST_CODE_INIT_THAI_ENGINE
    };

    /**
     * <pre>
     * See
     * - http://x0213.org/joyo-kanji-code/
     * - http://d.hatena.ne.jp/n-yuji/20120229/p1
     * - http://tama-san.com/?p=196
     * - https://code.google.com/p/language-detection/
     * </pre>
     */
    private static String getNotJoyoKanjiTest() {
        return ".*["
                + "\\p{InCJKUnifiedIdeographs}"
                + " &&[^"
                + "亜哀愛悪握圧扱安暗案以位依偉囲委威尉意慰易為異移維緯胃衣違遺医井域育一壱逸稲芋印員因姻引飲院陰隠韻右宇羽雨渦浦運雲営影映栄永泳英衛詠鋭液疫益駅悦謁越閲円園宴延援沿演炎煙猿縁遠鉛塩汚凹央奥往応押横欧殴王翁黄沖億屋憶乙卸恩温穏音下化仮何価佳加可夏嫁家寡科暇果架歌河火禍稼箇花荷華菓課貨過蚊我画芽賀雅餓介会解回塊壊快怪悔懐戒拐改械海灰界皆絵開階貝劾外害慨概涯街該垣嚇各拡格核殻獲確穫覚角較郭閣隔革学岳楽額掛潟割喝括活渇滑褐轄且株刈乾冠寒刊勘勧巻喚堪完官寛干幹患感慣憾換敢棺款歓汗漢環甘監看管簡緩缶肝艦観貫還鑑間閑関陥館丸含岸眼岩頑顔願企危喜器基奇寄岐希幾忌揮机旗既期棋棄機帰気汽祈季紀規記貴起軌輝飢騎鬼偽儀宜戯技擬欺犠疑義議菊吉喫詰却客脚虐逆丘久休及吸宮弓急救朽求泣球究窮級糾給旧牛去居巨拒拠挙虚許距漁魚享京供競共凶協叫境峡強恐恭挟教橋況狂狭矯胸脅興郷鏡響驚仰凝暁業局曲極玉勤均斤琴禁筋緊菌襟謹近金吟銀九句区苦駆具愚虞空偶遇隅屈掘靴繰桑勲君薫訓群軍郡係傾刑兄啓型契形径恵慶憩掲携敬景渓系経継茎蛍計警軽鶏芸迎鯨劇撃激傑欠決潔穴結血月件倹健兼券剣圏堅嫌建憲懸検権犬献研絹県肩見謙賢軒遣険顕験元原厳幻弦減源玄現言限個古呼固孤己庫弧戸故枯湖誇雇顧鼓五互午呉娯後御悟碁語誤護交侯候光公功効厚口向后坑好孔孝工巧幸広康恒慌抗拘控攻更校構江洪港溝甲皇硬稿紅絞綱耕考肯航荒行衡講貢購郊酵鉱鋼降項香高剛号合拷豪克刻告国穀酷黒獄腰骨込今困墾婚恨懇昆根混紺魂佐唆左差査砂詐鎖座債催再最妻宰彩才採栽歳済災砕祭斎細菜裁載際剤在材罪財坂咲崎作削搾昨策索錯桜冊刷察撮擦札殺雑皿三傘参山惨散桟産算蚕賛酸暫残仕伺使刺司史嗣四士始姉姿子市師志思指支施旨枝止死氏祉私糸紙紫肢脂至視詞詩試誌諮資賜雌飼歯事似侍児字寺慈持時次滋治璽磁示耳自辞式識軸七執失室湿漆疾質実芝舎写射捨赦斜煮社者謝車遮蛇邪借尺爵酌釈若寂弱主取守手朱殊狩珠種趣酒首儒受寿授樹需囚収周宗就州修愁拾秀秋終習臭舟衆襲週酬集醜住充十従柔汁渋獣縦重銃叔宿淑祝縮粛塾熟出術述俊春瞬准循旬殉準潤盾純巡遵順処初所暑庶緒署書諸助叙女序徐除傷償勝匠升召商唱奨宵将小少尚床彰承抄招掌昇昭晶松沼消渉焼焦照症省硝礁祥称章笑粧紹肖衝訟証詔詳象賞鐘障上丈乗冗剰城場壌嬢常情条浄状畳蒸譲醸錠嘱飾植殖織職色触食辱伸信侵唇娠寝審心慎振新森浸深申真神紳臣薪親診身辛進針震人仁刃尋甚尽迅陣酢図吹垂帥推水炊睡粋衰遂酔随髄崇数枢据杉澄寸世瀬畝是制勢姓征性成政整星晴正清牲生盛精聖声製西誠誓請逝青静斉税隻席惜斥昔析石積籍績責赤跡切拙接摂折設窃節説雪絶舌仙先千占宣専川戦扇栓泉浅洗染潜旋線繊船薦践選遷銭鮮前善漸然全禅繕塑措疎礎祖租粗素組訴阻僧創双倉喪壮奏層想捜掃挿操早曹巣槽燥争相窓総草荘葬藻装走送遭霜騒像増憎臓蔵贈造促側則即息束測足速俗属賊族続卒存孫尊損村他多太堕妥惰打駄体対耐帯待怠態替泰滞胎袋貸退逮隊代台大第題滝卓宅択拓沢濯託濁諾但達奪脱棚谷丹単嘆担探淡炭短端胆誕鍛団壇弾断暖段男談値知地恥池痴稚置致遅築畜竹蓄逐秩窒茶嫡着中仲宙忠抽昼柱注虫衷鋳駐著貯丁兆帳庁弔張彫徴懲挑朝潮町眺聴腸調超跳長頂鳥勅直朕沈珍賃鎮陳津墜追痛通塚漬坪釣亭低停偵貞呈堤定帝底庭廷弟抵提程締艇訂逓邸泥摘敵滴的笛適哲徹撤迭鉄典天展店添転点伝殿田電吐塗徒斗渡登途都努度土奴怒倒党冬凍刀唐塔島悼投搭東桃棟盗湯灯当痘等答筒糖統到討謄豆踏逃透陶頭騰闘働動同堂導洞童胴道銅峠匿得徳特督篤毒独読凸突届屯豚曇鈍内縄南軟難二尼弐肉日乳入如尿任妊忍認寧猫熱年念燃粘悩濃納能脳農把覇波派破婆馬俳廃拝排敗杯背肺輩配倍培媒梅買売賠陪伯博拍泊白舶薄迫漠爆縛麦箱肌畑八鉢発髪伐罰抜閥伴判半反帆搬板版犯班畔繁般藩販範煩頒飯晩番盤蛮卑否妃彼悲扉批披比泌疲皮碑秘罷肥被費避非飛備尾微美鼻匹必筆姫百俵標氷漂票表評描病秒苗品浜貧賓頻敏瓶不付夫婦富布府怖扶敷普浮父符腐膚譜負賦赴附侮武舞部封風伏副復幅服福腹複覆払沸仏物分噴墳憤奮粉紛雰文聞丙併兵塀幣平弊柄並閉陛米壁癖別偏変片編辺返遍便勉弁保舗捕歩補穂募墓慕暮母簿倣俸包報奉宝峰崩抱放方法泡砲縫胞芳褒訪豊邦飽乏亡傍剖坊妨帽忘忙房暴望某棒冒紡肪膨謀貿防北僕墨撲朴牧没堀奔本翻凡盆摩磨魔麻埋妹枚毎幕膜又抹末繭万慢満漫味未魅岬密脈妙民眠務夢無矛霧婿娘名命明盟迷銘鳴滅免綿面模茂妄毛猛盲網耗木黙目戻問紋門夜野矢厄役約薬訳躍柳愉油癒諭輸唯優勇友幽悠憂有猶由裕誘遊郵雄融夕予余与誉預幼容庸揚揺擁曜様洋溶用窯羊葉要謡踊陽養抑欲浴翌翼羅裸来頼雷絡落酪乱卵欄濫覧利吏履理痢裏里離陸律率立略流留硫粒隆竜慮旅虜了僚両寮料涼猟療糧良量陵領力緑倫厘林臨輪隣塁涙累類令例冷励礼鈴隷零霊麗齢暦歴列劣烈裂廉恋練連錬炉路露労廊朗楼浪漏老郎六録論和話賄惑枠湾腕挨曖宛嵐畏萎椅彙茨咽淫唄鬱怨媛艶旺岡臆俺苛牙瓦楷潰諧崖蓋骸柿顎葛釜鎌韓玩伎亀毀畿臼嗅巾僅錦惧串窟熊詣憬稽隙桁拳鍵舷股虎錮勾梗喉乞傲駒頃痕沙挫采塞埼柵刹拶斬恣摯餌鹿𠮟嫉腫呪袖羞蹴憧拭尻芯腎須裾凄醒脊戚煎羨腺詮箋膳狙遡曽爽痩踪捉遜汰唾堆戴誰旦綻緻酎貼嘲捗椎爪鶴諦溺塡妬賭藤瞳栃頓貪丼那奈梨謎鍋匂虹捻罵剝箸氾汎阪斑眉膝肘訃阜蔽餅璧蔑哺蜂貌頰睦勃昧枕蜜冥麺冶弥闇喩湧妖瘍沃拉辣藍璃慄侶瞭瑠呂賂弄籠麓脇"
                + "]"
                + "]+.*";
    }

    private static final String HIRAGANA_KATAKANA_TEST = ".*[\\p{InHiragana}\\p{InKatakana}]+.*";
    private static final String CHINESE_CHARACTERS_TEST = ".*[\\p{InCJKUnifiedIdeographs}]+.*";
    private static final String KOREAN_TEST = ".*[가-힣]+.*";
    private static final String THAI_TEST = ".*[\\p{InThai}]+.*";
    private static final String FRENCH_TEST = ".*[ÉÀÈÙÂÊÎÔÛËÏÜÇŒÆéàèùâêîôûëïüçœæ]+.*";

    public static final class ENGINE {
        public static final String GOOGLE_TTS = "com.google.android.tts";
        public static final String SAMSUNG_SMT = "com.samsung.SMT";
        public static final String IVONA_TTS = "com.ivona.tts";
        public static final String SVOX_CLLASIC = "com.svox.classic";
        public static final String SVOX_PICO = "com.svox.pico";
        public static final String AQUESTALK = "com.a_quest.aquestalka";
        public static final String N2_TTS = "jp.kddilabs.n2tts";
        public static final String VAJA_TTS = "com.spt.tts.vaja";
        public static final String DTALKER = "jp.co.createsystem";
        public static final String DTALKER_DEMO = "jp.co.createsystem.DTalkerTtsDemo";

        public static final String ENGLISH_ENGINE = GOOGLE_TTS;
        public static final String FRENCH_ENGINE = GOOGLE_TTS;
        public static final String JAPANESE_ENGINE = AQUESTALK;
        public static final String KOREAN_ENGINE = GOOGLE_TTS;
        public static final String CHINESE_ENGINE = SVOX_CLLASIC;
        public static final String THAI_ENGINE = SVOX_CLLASIC;
    }

    @Pref
    Preferences_ preferences;

    @RootContext
    Activity activity;

    private TextToSpeech englishTts = null;

    private TextToSpeech frenchTts = null;

    private TextToSpeech japaneseTts = null;

    private TextToSpeech koreanTts = null;

    private TextToSpeech chineseTts = null;

    private TextToSpeech thaiTts = null;

    private Locale initializingLocale = null;

    private String suspendedText = null;

    private List<Pair<Locale, TextToSpeech>> getPairsOfLocaleAndTts() {
        return Arrays.asList(
                new Pair<Locale, TextToSpeech>(Locale.ENGLISH, englishTts),
                new Pair<Locale, TextToSpeech>(Locale.FRENCH, frenchTts),
                new Pair<Locale, TextToSpeech>(Locale.JAPANESE, japaneseTts),
                new Pair<Locale, TextToSpeech>(Locale.CHINESE, chineseTts),
                new Pair<Locale, TextToSpeech>(Locale.KOREAN, koreanTts),
                new Pair<Locale, TextToSpeech>(THAI_LOCALE, thaiTts));
    }

    private String getEngineNameForLocale(Locale locale) {
        if (locale.equals(Locale.ENGLISH)) {
            return ENGINE.ENGLISH_ENGINE;
        }
        if (locale.equals(Locale.FRENCH)) {
            return ENGINE.FRENCH_ENGINE;
        }
        if (locale.equals(Locale.JAPANESE)) {
            return ENGINE.JAPANESE_ENGINE;
        }
        if (locale.equals(Locale.CHINESE)) {
            return ENGINE.CHINESE_ENGINE;
        }
        if (locale.equals(Locale.KOREAN)) {
            return ENGINE.KOREAN_ENGINE;
        }
        if (locale.equals(THAI_LOCALE)) {
            return ENGINE.THAI_ENGINE;
        }
        return null;
    }

    private int getRequestCodeForLocale(Locale locale) {
        if (locale.equals(Locale.ENGLISH)) {
            return REQUEST_CODE_INIT_ENGLISH_ENGINE;
        }
        if (locale.equals(Locale.FRENCH)) {
            return REQUEST_CODE_INIT_FRENCH_ENGINE;
        }
        if (locale.equals(Locale.JAPANESE)) {
            return REQUEST_CODE_INIT_JAPANESE_ENGINE;
        }
        if (locale.equals(Locale.CHINESE)) {
            return REQUEST_CODE_INIT_CHINESE_ENGINE;
        }
        if (locale.equals(Locale.KOREAN)) {
            return REQUEST_CODE_INIT_KOREAN_ENGINE;
        }
        if (locale.equals(THAI_LOCALE)) {
            return REQUEST_CODE_INIT_THAI_ENGINE;
        }
        return 0;
    }

    private void setTtsForLocale(Locale locale, TextToSpeech tts) {
         if (locale.equals(Locale.ENGLISH)) {
             englishTts = tts;
        }
        if (locale.equals(Locale.FRENCH)) {
            frenchTts = tts;
        }
        if (locale.equals(Locale.JAPANESE)) {
            japaneseTts = tts;
        }
        if (locale.equals(Locale.CHINESE)) {
            chineseTts = tts;
        }
        if (locale.equals(Locale.KOREAN)) {
            koreanTts = tts;
        }
        if (locale.equals(THAI_LOCALE)) {
            thaiTts = tts;
        }
    }

    private void init(Locale locale) {
        int requestCode = getRequestCodeForLocale(locale);
        Log.d(TAG, String.format("init: local=%s code=%d", locale.getDisplayName(), requestCode));
        initializingLocale = locale;
        showProgressMessage(initializingLocale.getDisplayName() + " TTS");
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        activity.startActivityForResult(intent, requestCode);
    }

    @UiThread
    void showProgressMessage(String target) {
        String message = activity.getResources().getString(R.string.message_in_processing);
        if (target != null) {
            message = target + " " + message;
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            Intent installIntent = new Intent();
            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            activity.startActivity(installIntent);
            return;
        }
        for (Pair<Locale, TextToSpeech> pair : getPairsOfLocaleAndTts()) {
            Locale locale = pair.first;
            TextToSpeech tts = pair.second;
            int requestCodeForLocale = getRequestCodeForLocale(locale);
            String engineName = getEngineNameForLocale(locale);

            if (requestCode == requestCodeForLocale) {
                tts = new TextToSpeech(activity, this, engineName);
                if (tts != null) {
                    setTtsForLocale(locale, tts);
                    showProgressMessage(engineName);
                    return;
                }
                showLanguageIsNotSupported(locale);
                return;
            }
        }
    }

    @UiThread
    void showLanguageIsNotSupported(Locale locale) {
        Toast.makeText(
                activity,
                activity.getResources().getString(R.string.message_item_is_not_supported, locale.getDisplayName()),
                Toast.LENGTH_LONG).show();
    }

    public boolean isProcessing() {
        if (suspendedText != null) {
            return true;
        }
        for (Pair<Locale, TextToSpeech> pair : getPairsOfLocaleAndTts()) {
            TextToSpeech tts = pair.second;
            if (tts != null && tts.isSpeaking()) {
                return true;
            }
        }
        return false;
    }

    public void speech(String text) {
        if (text.matches(HIRAGANA_KATAKANA_TEST)) {
            speech(text, Locale.JAPANESE);
            return;
        }
        if (text.matches(KOREAN_TEST)) {
            speech(text, Locale.KOREAN);
            return;
        }
        if (text.matches(THAI_TEST)) {
            speech(text, THAI_LOCALE);
            return;
        }
        if (text.matches(CHINESE_CHARACTERS_TEST)) {
            if (text.matches(getNotJoyoKanjiTest())) {
                speech(text, Locale.CHINESE);
            } else {
                speech(text, Locale.JAPANESE);
            }
            return;
        }
        if (text.matches(FRENCH_TEST)) {
            speech(text, Locale.FRENCH);
            return;
        }

        String localeText = preferences.ttsDefaultLocale().get().substring(0, 2).toLowerCase();
        if (localeText.equals("ch")) {
            localeText = "zh";
        }
        Locale defaultLocale = new Locale(localeText);
        if (defaultLocale == null) {
            defaultLocale = Locale.ENGLISH;
        }
        Log.d(TAG, "Default locale selected: " + defaultLocale.getDisplayName());
        speech(text, defaultLocale);
    }

    private HashMap<String, String> buildSpeakParams() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DateTimeUtils.getInstance().getCurrentDateTimeString());
        return params;
    }

    @Background
    public void speech(String text, Locale speechLocale) {
        Log.d(TAG, String.format("speech: (%s) %s", speechLocale.getDisplayName(), text));

        for (Pair<Locale, TextToSpeech> pair : getPairsOfLocaleAndTts()) {
            Locale locale = pair.first;
            TextToSpeech tts = pair.second;

            if (speechLocale.equals(locale)) {
                if (tts == null) {
                    if (suspendedText == null) {
                        suspendedText = text;
                        init(locale);
                        return;
                    }
                    showLanguageIsNotSupported(speechLocale);
                    return;
                }
                tts.setLanguage(speechLocale);
                tts.speak(text, TextToSpeech.QUEUE_ADD, buildSpeakParams());
                return;
            }
        }

        showLanguageIsNotSupported(speechLocale);
    }

    public void finish() {
        for (Pair<Locale, TextToSpeech> pair : getPairsOfLocaleAndTts()) {
            TextToSpeech tts = pair.second;
            if (tts != null) {
                tts.shutdown();
            }
        }
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit: " + status);
        if (status == TextToSpeech.ERROR) {
            suspendedText = null;
            showLanguageIsNotSupported(initializingLocale);
            return;
        }

        for (Pair<Locale, TextToSpeech> pair : getPairsOfLocaleAndTts()) {
            Locale locale = pair.first;
            TextToSpeech tts = pair.second;
            if (tts != null
                    && initializingLocale.equals(locale)
                    && tts.isLanguageAvailable(initializingLocale) == TextToSpeech.LANG_AVAILABLE) {

                if (suspendedText != null) {
                    speech(new String(suspendedText), initializingLocale);
                    suspendedText = null;
                }
                return;
            }
        }
        suspendedText = null;
        showLanguageIsNotSupported(initializingLocale);
    }
}
