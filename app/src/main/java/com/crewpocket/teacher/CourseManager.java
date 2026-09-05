package com.crewpocket.teacher;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseManager {

    private static final String PREF_NAME = "crew_course_progress";
    private static List<CourseModel.Track> cachedTracks = null;
    private static Map<String, CourseModel.Lesson> cachedLessonsMap = null;

    public static synchronized List<CourseModel.Track> getTracks() {
        if (cachedTracks == null) {
            initTracks();
        }
        return cachedTracks;
    }

    public static synchronized CourseModel.Lesson getLessonById(String lessonId) {
        if (cachedLessonsMap == null) {
            getTracks(); // ensures cache is populated
        }
        return cachedLessonsMap != null ? cachedLessonsMap.get(lessonId) : null;
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static CourseModel.LessonProgress getLessonProgress(Context context, String lessonId) {
        CourseModel.LessonProgress prog = new CourseModel.LessonProgress();
        prog.lessonId = lessonId;
        try {
            String jsonStr = getPrefs(context).getString("prog_" + lessonId, null);
            if (jsonStr != null) {
                JSONObject obj = new JSONObject(jsonStr);
                prog.stars = obj.optInt("stars", 0);
                prog.bestScore = obj.optInt("bestScore", 0);
                prog.completed = obj.optBoolean("completed", false);
                prog.lastCompletedTime = obj.optLong("lastCompletedTime", 0);
            }
        } catch (Exception ignored) {}
        return prog;
    }

    public static synchronized void saveLessonProgress(Context context, String lessonId, int stars, int score) {
        if (context == null || lessonId == null) return;
        CourseModel.LessonProgress current = getLessonProgress(context, lessonId);
        int newStars = Math.max(current.stars, stars);
        int newScore = Math.max(current.bestScore, score);
        boolean completed = current.completed || (stars >= 1);

        try {
            JSONObject obj = new JSONObject();
            obj.put("lessonId", lessonId);
            obj.put("stars", newStars);
            obj.put("bestScore", newScore);
            obj.put("completed", completed);
            obj.put("lastCompletedTime", System.currentTimeMillis());

            getPrefs(context).edit().putString("prog_" + lessonId, obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static boolean isLessonUnlocked(Context context, String lessonId) {
        if (lessonId == null) return false;
        List<CourseModel.Track> tracks = getTracks();
        
        // Find which lesson and unit this belongs to
        for (CourseModel.Track track : tracks) {
            for (int u = 0; u < track.units.size(); u++) {
                CourseModel.Unit unit = track.units.get(u);
                for (int l = 0; l < unit.lessons.size(); l++) {
                    CourseModel.Lesson lesson = unit.lessons.get(l);
                    if (lesson.id.equals(lessonId)) {
                        // First lesson in the very first unit is always unlocked
                        if (u == 0 && l == 0) return true;

                        // First lesson of subsequent units unlocked if previous unit's last lesson is completed
                        if (l == 0) {
                            CourseModel.Unit prevUnit = track.units.get(u - 1);
                            if (prevUnit.lessons.isEmpty()) return true;
                            CourseModel.Lesson prevLastLesson = prevUnit.lessons.get(prevUnit.lessons.size() - 1);
                            return getLessonProgress(context, prevLastLesson.id).completed;
                        }

                        // Subsequent lesson in same unit unlocked if previous lesson is completed
                        CourseModel.Lesson prevLesson = unit.lessons.get(l - 1);
                        return getLessonProgress(context, prevLesson.id).completed;
                    }
                }
            }
        }
        return true;
    }

    public static int getTotalStars(Context context) {
        int total = 0;
        if (cachedLessonsMap == null) getTracks();
        if (cachedLessonsMap != null) {
            for (String lessonId : cachedLessonsMap.keySet()) {
                total += getLessonProgress(context, lessonId).stars;
            }
        }
        return total;
    }

    public static int getCompletedLessonsCount(Context context) {
        int count = 0;
        if (cachedLessonsMap == null) getTracks();
        if (cachedLessonsMap != null) {
            for (String lessonId : cachedLessonsMap.keySet()) {
                if (getLessonProgress(context, lessonId).completed) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int getTotalLessonsCount() {
        if (cachedLessonsMap == null) getTracks();
        return cachedLessonsMap != null ? cachedLessonsMap.size() : 0;
    }

    private static void initTracks() {
        cachedTracks = new ArrayList<>();
        cachedLessonsMap = new HashMap<>();

        // ════════════════════════════════════════════════════════════════════
        // ✈️ Track 1: Travel & Survival
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track travelTrack = new CourseModel.Track(
                "travel", "✈️",
                "Travel & Survival English", "出國自由行與生活生存英語",
                "Master ordering, transit, hotels and airport communications easily.",
                "從點餐、機場海關、交通問路到飯店入住，輕鬆掌握海外生存必備口語。"
        );

        // Unit 1: Restaurant & Cafe
        CourseModel.Unit u1 = new CourseModel.Unit("travel_u1", "travel",
                "Unit 1: Dining & Cafe Ordering", "第 1 單元：咖啡廳與餐廳點餐",
                "Learn to order drinks, steak, customized dietary options, and bill splitting.",
                "學會飲品客製、牛排熟度點選、過敏忌口告知與結帳買單。");

        // Lesson 1.1
        CourseModel.Lesson l1_1 = new CourseModel.Lesson("travel_u1_l1", "travel", "travel_u1",
                "Coffee Shop & Drink Customization", "咖啡廳點餐與客製化甜度冰塊",
                "Order coffee and tea drinks with milk choices and sweetness levels.",
                "點選咖啡與茶飲，並提出換燕麥奶、甜度冰塊等客製要求。",
                "food_ordering",
                "You are a friendly barista at a specialty coffee shop. Greet the customer warmly and ask for their drink order, size, and milk preference.");
        l1_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I'd like an iced latte with oat milk, please.",
                "請給我一杯冰拿鐵，換燕麥奶。",
                "/aɪd laɪk ən aɪst ˈlɑːteɪ wɪð oʊt mɪlk pliːz/",
                "點餐必備：I'd like a [餐點] with [要求]"));
        l1_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Could I get that with less ice and half sugar?",
                "可以幫我做少冰、半糖嗎？",
                "/kʊd aɪ ɡɛt ðæt wɪð lɛs aɪs ænd hæf ˈʃʊɡər/",
                "less ice = 少冰, half sugar = 半糖, no ice = 去冰"));
        l1_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Can I pay with Apple Pay / credit card?",
                "可以用 Apple Pay 或信用卡結帳嗎？",
                "/kæn aɪ peɪ wɪð ˈæp.əl peɪ / ˈkrɛdɪt kɑːrd/",
                "行動支付與結帳常用句"));
        l1_1.missions.add(new CourseModel.Mission(1, "向店員點一杯咖啡或茶飲", "向店員點一杯咖啡或茶飲", new String[]{"latte", "coffee", "tea", "cappuccino", "order", "like"}));
        l1_1.missions.add(new CourseModel.Mission(2, "提出客製化要求（換奶/甜度/冰塊）", "提出客製化要求（換奶/甜度/冰塊）", new String[]{"oat milk", "less ice", "half sugar", "no ice", "skim", "almond", "sugar", "ice"}));
        l1_1.missions.add(new CourseModel.Mission(3, "詢問結帳方式並完成買單", "詢問結帳方式並完成買單", new String[]{"pay", "card", "apple pay", "cash", "total", "how much"}));
        u1.lessons.add(l1_1);

        // Lesson 1.2
        CourseModel.Lesson l1_2 = new CourseModel.Lesson("travel_u1_l2", "travel", "travel_u1",
                "Steakhouse & Food Recommendations", "西餐廳點餐與牛排熟度",
                "Ask for house specials, choose steak doneness, and request side dishes.",
                "詢問主廚招牌菜、點選牛排熟度與搭配附餐。",
                "food_ordering",
                "You are an attentive waiter at an upscale steakhouse. Welcome the guest, introduce today's special, and ask for their steak doneness preference.");
        l1_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "What do you recommend for the main course?",
                "主菜你們有什麼推薦的嗎？",
                "/wʌt du juː ˌrɛkəˈmɛnd fɔːr ðə meɪn kɔːrs/",
                "詢問推薦：What do you recommend?"));
        l1_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I'll have the ribeye steak, medium-rare, please.",
                "我要一份肋眼牛排，五分熟，謝謝。",
                "/aɪl hæv ðə ˈrɪb.aɪ steɪk, ˈmiːdiəm rɛər, pliːz/",
                "熟度：rare(三分), medium-rare(五分), medium(七分), well-done(全熟)"));
        l1_2.missions.add(new CourseModel.Mission(1, "詢問服務生推薦料理或招牌菜", "詢問服務生推薦料理或招牌菜", new String[]{"recommend", "special", "popular", "signature"}));
        l1_2.missions.add(new CourseModel.Mission(2, "點選牛排並指定熟度 (如 medium-rare)", "點選牛排並指定熟度 (如 medium-rare)", new String[]{"medium-rare", "medium", "rare", "well-done", "steak", "ribeye"}));
        l1_2.missions.add(new CourseModel.Mission(3, "選擇附餐或詢問醬汁搭配", "選擇附餐或詢問醬汁搭配", new String[]{"sauce", "fries", "salad", "side", "potato", "mash"}));
        u1.lessons.add(l1_2);

        // Lesson 1.3
        CourseModel.Lesson l1_3 = new CourseModel.Lesson("travel_u1_l3", "travel", "travel_u1",
                "Handling Food Issues & Splitting the Bill", "餐點問題反映與帳單分攤",
                "Politely report a wrong dish and ask to split the bill with friends.",
                "禮貌反映送錯餐點或冷掉，並與服務生提出分開結帳。",
                "food_ordering",
                "You are a polite restaurant manager. Listen to the customer's request carefully, apologize if needed, and assist with bill payment.");
        l1_3.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Excuse me, I think this isn't what I ordered.",
                "不好意思，這好像不是我點的餐點。",
                "/ɪkˈskjuːz miː, aɪ θɪŋk ðɪs ˈɪznt wʌt aɪ ˈɔːrdərd/",
                "委婉禮貌反映錯誤必備"));
        l1_3.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Could we get the bill and split it separately?",
                "可以幫我們結帳並分開付嗎？",
                "/kʊd wiː ɡɛt ðə bɪl ænd splɪt ɪt ˈsɛpəreɪtli/",
                "split the bill = 分開付 / AA制"));
        l1_3.missions.add(new CourseModel.Mission(1, "禮貌反映餐點狀況（如送錯或冷掉）", "禮貌反映餐點狀況（如送錯或冷掉）", new String[]{"wrong", "cold", "order", "ordered", "mistake", "excuse me"}));
        l1_3.missions.add(new CourseModel.Mission(2, "要求索取帳單 (Check / Bill)", "要求索取帳單 (Check / Bill)", new String[]{"bill", "check", "receipt"}));
        l1_3.missions.add(new CourseModel.Mission(3, "提出分開結帳 (Split the bill)", "提出分開結帳 (Split the bill)", new String[]{"split", "separately", "separate", "each"}));
        u1.lessons.add(l1_3);

        travelTrack.units.add(u1);

        // Unit 2: Airport & Transportation
        CourseModel.Unit u2 = new CourseModel.Unit("travel_u2", "travel",
                "Unit 2: Airport & Transit", "第 2 單元：機場出入境與交通問路",
                "Check-in, customs immigration, baggage claim, and asking for directions.",
                "機場報到劃位、海關入境應答、行李問題與街頭問路。");

        CourseModel.Lesson l2_1 = new CourseModel.Lesson("travel_u2_l1", "travel", "travel_u2",
                "Airport Check-in & Window Seat", "機場報到劃位與更換靠窗座位",
                "Check in your bags and ask the ground staff for a window or aisle seat.",
                "在機場櫃檯辦理登機報到、托運行李並要求靠窗或靠走道座位。",
                "airport",
                "You are an airline check-in ground agent. Greet the passenger, ask for passport, baggage details, and seat preference.");
        l2_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Could I have a window seat, please?",
                "可以給我一個靠窗的座位嗎？",
                "/kʊd aɪ hæv ə ˈwɪndoʊ siːt pliːz/",
                "window seat = 靠窗, aisle seat = 靠走道"));
        l2_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I have one piece of check-in luggage and one carry-on.",
                "我有一件托運行李和一件隨身行李。",
                "/aɪ hæv wʌn piːs ʌv ˈtʃɛk.ɪn ˈlʌɡɪdʒ ænd wʌn ˈkæri.ɒn/",
                "check-in luggage = 托運行李, carry-on = 隨身行李"));
        l2_1.missions.add(new CourseModel.Mission(1, "主動出示護照並表明飛往目的地", "主動出示護照並表明飛往目的地", new String[]{"passport", "flying", "flight", "to", "here is"}));
        l2_1.missions.add(new CourseModel.Mission(2, "說明行李托運數量 (Check-in luggage)", "說明行李托運數量 (Check-in luggage)", new String[]{"luggage", "bag", "bags", "check-in", "suitcase", "piece"}));
        l2_1.missions.add(new CourseModel.Mission(3, "要求靠窗或靠走道座位 (Window/Aisle seat)", "要求靠窗或靠走道座位 (Window/Aisle seat)", new String[]{"window", "aisle", "seat"}));
        u2.lessons.add(l2_1);

        CourseModel.Lesson l2_2 = new CourseModel.Lesson("travel_u2_l2", "travel", "travel_u2",
                "Customs Immigration & Travel Purpose", "海關入境問答與旅遊目的",
                "Answer customs officer questions clearly about your stay and return ticket.",
                "向海關官員清晰回答入境目的、停留天數與回程機票。",
                "airport",
                "You are an immigration customs officer at border control. Ask the traveler about their visit purpose, duration of stay, and accommodation.");
        l2_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I'm here on vacation / sightseeing for 7 days.",
                "我是來觀光度假的，預計停留 7 天。",
                "/aɪm hɪər ɒn veɪˈkeɪʃən / ˈsaɪtˌsiːɪŋ fɔːr ˈsɛvən deɪz/",
                "入境目的：vacation (度假), sightseeing (觀光), business (商務)"));
        l2_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I'll be staying at the Hilton Hotel downtown.",
                "我會住在市區的希爾頓飯店。",
                "/aɪl biː ˈsteɪɪŋ æt ðə ˈhɪltən hoʊˈtɛl ˈdaʊnˌtaʊn/",
                "住宿地點說明句型"));
        l2_2.missions.add(new CourseModel.Mission(1, "說明入境目的（如觀光旅遊/度假）", "說明入境目的（如觀光旅遊/度假）", new String[]{"vacation", "sightseeing", "holiday", "travel", "tourism", "visit"}));
        l2_2.missions.add(new CourseModel.Mission(2, "告知停留天數與住宿飯店名稱", "告知停留天數與住宿飯店名稱", new String[]{"stay", "days", "week", "hotel", "staying"}));
        l2_2.missions.add(new CourseModel.Mission(3, "表明已訂好回程機票 (Return ticket)", "表明已訂好回程機票 (Return ticket)", new String[]{"return", "ticket", "flight back", "leaving"}));
        u2.lessons.add(l2_2);

        CourseModel.Lesson l2_3 = new CourseModel.Lesson("travel_u2_l3", "travel", "travel_u2",
                "Subway & Street Directions", "地鐵購票與街頭問路",
                "Ask pedestrians for directions to the train station or landmark.",
                "向路人詢問地鐵站方向、換乘路線與步行時間。",
                "travel",
                "You are a helpful local pedestrian in London/New York. Guide the traveler to their destination clearly.");
        l2_3.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Excuse me, how do I get to the nearest subway station?",
                "不好意思，請問最近的地鐵站怎麼走？",
                "/ɪkˈskjuːz miː, haʊ duː aɪ ɡɛt tuː ðə ˈnɪərɪst ˈsʌbweɪ ˈsteɪʃən/",
                "問路經典句：How do I get to [地點]?"));
        l2_3.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Is it within walking distance from here?",
                "從這裡走路能到嗎？",
                "/ɪz ɪt wɪˈðɪn ˈwɔːkɪŋ ˈdɪstəns frʌm hɪər/",
                "確認步行距離實用句"));
        l2_3.missions.add(new CourseModel.Mission(1, "禮貌詢問前往某地/地鐵站的方向", "禮貌詢問前往某地/地鐵站的方向", new String[]{"how do i get", "where is", "station", "way to", "subway", "metro"}));
        l2_3.missions.add(new CourseModel.Mission(2, "詢問步行或搭車大約需要多久 (How long)", "詢問步行或搭車大約需要多久 (How long)", new String[]{"how long", "minutes", "walk", "far", "distance"}));
        l2_3.missions.add(new CourseModel.Mission(3, "致謝並確認方向 (Thank you)", "致謝並確認方向 (Thank you)", new String[]{"thank you", "thanks", "appreciate", "got it"}));
        u2.lessons.add(l2_3);

        travelTrack.units.add(u2);

        // Unit 3: Hotel & Stay
        CourseModel.Unit u3 = new CourseModel.Unit("travel_u3", "travel",
                "Unit 3: Hotel & Accommodation", "第 3 單元：飯店入住與設施要求",
                "Check-in, room problems, luggage storage, and check-out.",
                "飯店 Check-in、要求高樓層、房間設備問題反映與行李寄存。");

        CourseModel.Lesson l3_1 = new CourseModel.Lesson("travel_u3_l1", "travel", "travel_u3",
                "Hotel Check-in & Quiet High Floor", "飯店 Check-in 與要求高樓層安靜房",
                "Check into your hotel room and request a quiet room on a high floor.",
                "辦理飯店入住，確認早餐時間並要求高樓層安靜房間。",
                "hotel",
                "You are a receptionist at a boutique hotel. Welcome the guest, confirm their booking, and assist with room keys.");
        l3_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Hi, I have a reservation under the name John Smith.",
                "嗨，我有一筆訂房，登記的名字是 John Smith。",
                "/haɪ, aɪ hæv ə ˌrɛzərˈveɪʃən ˈʌndər ðə neɪm dʒɒn smɪθ/",
                "入住開場必備：I have a reservation under [姓名]"));
        l3_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "If possible, could we get a quiet room on a higher floor?",
                "如果可以的話，能給我們高樓層安靜一點的房間嗎？",
                "/ɪf ˈpɒsəbl, kʊd wiː ɡɛt ə ˈkwaɪət ruːm ɒn ə ˈhaɪər flɔːr/",
                "客製客房要求實用句"));
        l3_1.missions.add(new CourseModel.Mission(1, "出示預訂並說明入住姓名 (Reservation)", "出示預訂並說明入住姓名 (Reservation)", new String[]{"reservation", "booking", "check in", "name", "booked"}));
        l3_1.missions.add(new CourseModel.Mission(2, "要求高樓層或安靜房間 (High floor / Quiet)", "要求高樓層或安靜房間 (High floor / Quiet)", new String[]{"high floor", "higher floor", "quiet", "view", "bed"}));
        l3_1.missions.add(new CourseModel.Mission(3, "詢問 Wi-Fi 密碼或早餐時間 (Wi-Fi / Breakfast)", "詢問 Wi-Fi 密碼或早餐時間 (Wi-Fi / Breakfast)", new String[]{"wifi", "wi-fi", "breakfast", "password", "time"}));
        u3.lessons.add(l3_1);

        CourseModel.Lesson l3_2 = new CourseModel.Lesson("travel_u3_l2", "travel", "travel_u3",
                "Room Maintenance & Requesting Room Change", "房間設備故障與要求換房",
                "Call the front desk to report broken air conditioning or hot water issues.",
                "致電櫃檯反映冷氣故障或沒有熱水，必要時要求換房。",
                "hotel",
                "You are the front desk agent on duty. Handle the guest's complaint with empathy and send assistance promptly.");
        l3_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "The air conditioning in room 402 isn't working at all.",
                "402 號房的冷氣完全不運轉。",
                "/ðiː ɛər kənˈdɪʃənɪŋ ɪn ruːm fɔːr oʊ tuː ˈɪznt ˈwɜːrkɪŋ æt ɔːl/",
                "設備故障：The [設備] isn't working"));
        l3_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Could you send someone up to check, or switch us to another room?",
                "可以派人上來檢查，或是幫我們換一間房間嗎？",
                "/kʊd juː sɛnd ˈsʌmwʌn ʌp tuː tʃɛk, ɔːr swɪtʃ ʌs tuː əˈnʌðər ruːm/",
                "要求解決或換房"));
        l3_2.missions.add(new CourseModel.Mission(1, "表明房號並清楚描述設備故障問題", "表明房號並清楚描述設備故障問題", new String[]{"room", "air conditioning", "ac", "hot water", "working", "broken", "noise"}));
        l3_2.missions.add(new CourseModel.Mission(2, "要求派維修人員上樓檢查 (Send someone)", "要求派維修人員上樓檢查 (Send someone)", new String[]{"send", "check", "fix", "look", "repair"}));
        l3_2.missions.add(new CourseModel.Mission(3, "提出若無法修復希望能更換房間 (Switch room)", "提出若無法修復希望能更換房間 (Switch room)", new String[]{"switch", "change", "another room", "different room"}));
        u3.lessons.add(l3_2);

        travelTrack.units.add(u3);
        cachedTracks.add(travelTrack);

        // ════════════════════════════════════════════════════════════════════
        // 💼 Track 2: Business English
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track bizTrack = new CourseModel.Track(
                "business", "💼",
                "Business & Career English", "職場商務與跨國溝通",
                "Master meetings, status reports, polite negotiations, and networking.",
                "掌握英文會議主持、進度匯報、禮貌協商、面試與商業社交談判技巧。"
        );

        CourseModel.Unit bu1 = new CourseModel.Unit("biz_u1", "business",
                "Unit 1: Remote Meetings & Updates", "第 1 單元：跨國線上會議與專案進度",
                "Icebreaking in meetings, delivering status updates, and alignment.",
                "線上會議開場破冰、專案進度匯報與排除阻礙。");

        CourseModel.Lesson bl1_1 = new CourseModel.Lesson("biz_u1_l1", "business", "biz_u1",
                "Meeting Icebreaker & Self Introduction", "會議開場破冰與自我介紹",
                "Introduce yourself concisely in an international team standup meeting.",
                "在跨國團隊會議中簡明自我介紹、說明職責與問候同仁。",
                "business",
                "You are chairing a global sync meeting. Ask the new teammate to introduce themselves and their role.");
        bl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "Good morning everyone, thrilled to join the team as the frontend lead.",
                "大家早安，很高興以資深前端負責人的身份加入團隊。",
                "/ɡʊd ˈmɔːrnɪŋ ˈɛvrɪwʌn, θrɪld tuː dʒɔɪn ðə tiːm æz ðə ˈfrʌntˌɛnd liːd/",
                "破冰自介：Thrilled to join as [職位]"));
        bl1_1.missions.add(new CourseModel.Mission(1, "向與會同仁打招呼並說明自己的角色 (Role)", "向與會同仁打招呼並說明自己的角色 (Role)", new String[]{"morning", "everyone", "role", "lead", "engineer", "designer", "manager", "name is"}));
        bl1_1.missions.add(new CourseModel.Mission(2, "簡要提及本季或近期負責的主要目標", "簡要提及本季或近期負責的主要目標", new String[]{"responsible", "focus", "working on", "goal", "project", "building"}));
        bl1_1.missions.add(new CourseModel.Mission(3, "表達期待與團隊合作 (Look forward to)", "表達期待與團隊合作 (Look forward to)", new String[]{"look forward", "working together", "collaborate", "excited", "happy to be here"}));
        bu1.lessons.add(bl1_1);

        CourseModel.Lesson bl1_2 = new CourseModel.Lesson("biz_u1_l2", "business", "biz_u1",
                "Project Status Updates & Blockers", "專案進度匯報與遭遇阻礙",
                "Give a crisp 2-minute status update on what's done and current blockers.",
                "清晰匯報已完成事項、進行中項目與需要團隊協助排除的阻礙 (Blockers)。",
                "business",
                "You are an agile project manager. Ask the engineer about their sprint progress and any blockers.");
        bl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "We've completed the API migration; currently we're testing the payment gateway.",
                "我們已經完成了 API 遷移，目前正在測試支付閘道。",
                "/wiːv kəmˈpliːtɪd ðiː ˌeɪ.piːˈaɪ maɪˈɡreɪʃən, ˈkɜːrəntli wɪər ˈtɛstɪŋ ðə ˈpeɪmənt ˈɡeɪtweɪ/",
                "進度句型：We've completed X, currently working on Y"));
        bl1_2.missions.add(new CourseModel.Mission(1, "說明最近已完成的里程碑 (Completed)", "說明最近已完成的里程碑 (Completed)", new String[]{"completed", "finished", "shipped", "done", "launched"}));
        bl1_2.missions.add(new CourseModel.Mission(2, "說明目前進行中的項目 (Currently working on)", "說明目前進行中的項目 (Currently working on)", new String[]{"currently", "working on", "testing", "building", "developing"}));
        bl1_2.missions.add(new CourseModel.Mission(3, "提出阻礙並尋求跨團隊支援 (Blocker / Need help)", "提出阻礙並尋求跨團隊支援 (Blocker / Need help)", new String[]{"blocker", "blocked", "need help", "sync", "support", "dependency"}));
        bu1.lessons.add(bl1_2);

        bizTrack.units.add(bu1);
        cachedTracks.add(bizTrack);

        // ════════════════════════════════════════════════════════════════════
        // ☕ Track 3: Daily Small Talk
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track dailyTrack = new CourseModel.Track(
                "daily", "☕",
                "Daily Conversation & Small Talk", "日常社交與深度閒聊",
                "Talk about weekends, movies, food, culture, and make genuine friends.",
                "聊週末計畫、影集美食、文化日常，自然開啟話題並結交新朋友。"
        );

        CourseModel.Unit du1 = new CourseModel.Unit("daily_u1", "daily",
                "Unit 1: Weekend Life & Hobbies", "第 1 單元：週末生活與興趣話題",
                "Sharing weekend stories, hobbies, movie picks, and rescheduling plans.",
                "分享週末行程、興趣運動、影集推薦與改期邀約。");

        CourseModel.Lesson dl1_1 = new CourseModel.Lesson("daily_u1_l1", "daily", "daily_u1",
                "Weekend Plans & Outdoor Hobbies", "週末活動與戶外休閒",
                "Chat casually with a friend about outdoor hiking, cafes, or relaxing plans.",
                "和朋友輕鬆聊聊週末的登山健行、探店咖啡廳或放鬆規劃。",
                "daily",
                "You are an energetic and friendly friend. Ask what your buddy did last weekend and share excitement about outdoor activities.");
        dl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase(
                "I went hiking in Yangmingshan; the weather was phenomenal!",
                "我去了陽明山爬山，天氣超級棒！",
                "/aɪ wɛnt ˈhaɪkɪŋ ɪn ..., ðə ˈwɛðər wʌz fɪˈnɒmɪnl/",
                "分享經歷：I went [活動], the [細節] was great!"));
        dl1_1.missions.add(new CourseModel.Mission(1, "分享自己上週末做了一件有趣的事", "分享自己上週末做了一件有趣的事", new String[]{"weekend", "went", "hiking", "movie", "cafe", "cooked", "played", "stayed"}));
        dl1_1.missions.add(new CourseModel.Mission(2, "主動反問對方的週末或近期生活 (How about you?)", "主動反問對方的週末或近期生活 (How about you?)", new String[]{"how about you", "what about you", "did you", "how was your"}));
        dl1_1.missions.add(new CourseModel.Mission(3, "約定下次一起參與某個活動 (Let's do that)", "約定下次一起參與某個活動 (Let's do that)", new String[]{"next time", "let's", "join", "together", "should do that", "sounds fun"}));
        du1.lessons.add(dl1_1);

        dailyTrack.units.add(du1);
        cachedTracks.add(dailyTrack);

        // Populate lessons map
        for (CourseModel.Track t : cachedTracks) {
            for (CourseModel.Unit u : t.units) {
                for (CourseModel.Lesson l : u.lessons) {
                    cachedLessonsMap.put(l.id, l);
                }
            }
        }
    }
}
