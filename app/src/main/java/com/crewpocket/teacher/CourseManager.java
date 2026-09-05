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
        
        for (CourseModel.Track track : tracks) {
            for (int u = 0; u < track.units.size(); u++) {
                CourseModel.Unit unit = track.units.get(u);
                for (int l = 0; l < unit.lessons.size(); l++) {
                    CourseModel.Lesson lesson = unit.lessons.get(l);
                    if (lesson.id.equals(lessonId)) {
                        if (u == 0 && l == 0) return true;

                        if (l == 0) {
                            CourseModel.Unit prevUnit = track.units.get(u - 1);
                            if (prevUnit.lessons.isEmpty()) return true;
                            CourseModel.Lesson prevLastLesson = prevUnit.lessons.get(prevUnit.lessons.size() - 1);
                            return getLessonProgress(context, prevLastLesson.id).completed;
                        }

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
        // ✈️ Track 1: Travel & Survival English (10 Lessons, 4 Units)
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track travelTrack = new CourseModel.Track(
                "travel", "✈️",
                "Travel & Survival English", "出國自由行與生活生存英語",
                "Master ordering, transit, hotel requests, customs and shopping easily.",
                "從點餐、機場海關、交通問路、飯店入住到購物退稅，全面掌握海外生活口語。"
        );

        // Unit 1: Restaurant & Dining
        CourseModel.Unit tu1 = new CourseModel.Unit("travel_u1", "travel",
                "Unit 1: Dining & Cafe Ordering", "第 1 單元：咖啡廳與各類餐廳點餐",
                "Learn drink customizations, steakhouse doneness, fast food and bill splitting.",
                "學會飲品客製、牛排熟度、速食店得來速、忌口交代與結帳買單。");

        // 1.1 Coffee Shop
        CourseModel.Lesson tl1_1 = new CourseModel.Lesson("travel_u1_l1", "travel", "travel_u1",
                "Coffee Shop & Drink Customization", "咖啡廳點餐與客製化甜度冰塊",
                "Order coffee and tea drinks with milk choices and sweetness levels.",
                "點選咖啡與茶飲，並提出換燕麥奶、甜度冰塊等客製要求。",
                "food_ordering",
                "You are a friendly barista at a specialty coffee shop. Greet the customer warmly and ask for their drink order, size, and milk preference.");
        tl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("I'd like an iced latte with oat milk, please.", "請給我一杯冰拿鐵，換燕麥奶。", "/aɪd laɪk ən aɪst ˈlɑːteɪ wɪð oʊt mɪlk pliːz/", "點餐必備句"));
        tl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Could I get that with less ice and half sugar?", "可以幫我做少冰、半糖嗎？", "/kʊd aɪ ɡɛt ðæt wɪð lɛs aɪs ænd hæf ˈʃʊɡər/", "客製甜度冰塊"));
        tl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Can I pay with Apple Pay or credit card?", "可以用 Apple Pay 或信用卡結帳嗎？", "/kæn aɪ peɪ wɪð ˈæp.əl peɪ / ˈkrɛdɪt kɑːrd/", "結帳付款方式"));
        tl1_1.missions.add(new CourseModel.Mission(1, "向店員點一杯咖啡或茶飲", "向店員點一杯咖啡或茶飲", new String[]{"latte", "coffee", "tea", "cappuccino", "order", "like"}));
        tl1_1.missions.add(new CourseModel.Mission(2, "提出客製化要求（換奶/甜度/冰塊）", "提出客製化要求（換奶/甜度/冰塊）", new String[]{"oat milk", "less ice", "half sugar", "no ice", "skim", "almond", "sugar", "ice"}));
        tl1_1.missions.add(new CourseModel.Mission(3, "詢問結帳方式並完成買單", "詢問結帳方式並完成買單", new String[]{"pay", "card", "apple pay", "cash", "total", "how much"}));
        tu1.lessons.add(tl1_1);

        // 1.2 Steakhouse
        CourseModel.Lesson tl1_2 = new CourseModel.Lesson("travel_u1_l2", "travel", "travel_u1",
                "Steakhouse & Food Recommendations", "西餐廳點餐與牛排熟度",
                "Ask for house specials, choose steak doneness, and request side dishes.",
                "詢問主廚招牌菜、點選牛排熟度與搭配附餐。",
                "food_ordering",
                "You are an attentive waiter at an upscale steakhouse. Welcome the guest, introduce today's special, and ask for their steak doneness preference.");
        tl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("What do you recommend for the main course?", "主菜你們有什麼推薦的嗎？", "/wʌt du juː ˌrɛkəˈmɛnd fɔːr ðə meɪn kɔːrs/", "詢問推薦"));
        tl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("I'll have the ribeye steak, medium-rare, please.", "我要一份肋眼牛排，五分熟，謝謝。", "/aɪl hæv ðə ˈrɪb.aɪ steɪk, ˈmiːdiəm rɛər, pliːz/", "熟度表達"));
        tl1_2.missions.add(new CourseModel.Mission(1, "詢問服務生推薦料理或招牌菜", "詢問服務生推薦料理或招牌菜", new String[]{"recommend", "special", "popular", "signature"}));
        tl1_2.missions.add(new CourseModel.Mission(2, "點選牛排並指定熟度 (如 medium-rare)", "點選牛排並指定熟度 (如 medium-rare)", new String[]{"medium-rare", "medium", "rare", "well-done", "steak", "ribeye"}));
        tl1_2.missions.add(new CourseModel.Mission(3, "選擇附餐或詢問醬汁搭配", "選擇附餐或詢問醬汁搭配", new String[]{"sauce", "fries", "salad", "side", "potato", "mash"}));
        tu1.lessons.add(tl1_2);

        // 1.3 Food Issues & Split Bill
        CourseModel.Lesson tl1_3 = new CourseModel.Lesson("travel_u1_l3", "travel", "travel_u1",
                "Handling Food Issues & Splitting the Bill", "餐點問題反映與帳單分攤",
                "Politely report a wrong dish and ask to split the bill with friends.",
                "禮貌反映送錯餐點或冷掉，並與服務生提出分開結帳。",
                "food_ordering",
                "You are a polite restaurant manager. Listen to the customer's request carefully, apologize if needed, and assist with bill payment.");
        tl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Excuse me, I think this isn't what I ordered.", "不好意思，這好像不是我點的餐點。", "/ɪkˈskjuːz miː, aɪ θɪŋk ðɪs ˈɪznt wʌt aɪ ˈɔːrdərd/", "禮貌反映錯誤"));
        tl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Could we get the bill and split it separately?", "可以幫我們結帳並分開付嗎？", "/kʊd wiː ɡɛt ðə bɪl ænd splɪt ɪt ˈsɛpəreɪtli/", "分開付 / AA制"));
        tl1_3.missions.add(new CourseModel.Mission(1, "禮貌反映餐點狀況（如送錯或冷掉）", "禮貌反映餐點狀況（如送錯或冷掉）", new String[]{"wrong", "cold", "order", "ordered", "mistake", "excuse me"}));
        tl1_3.missions.add(new CourseModel.Mission(2, "要求索取帳單 (Check / Bill)", "要求索取帳單 (Check / Bill)", new String[]{"bill", "check", "receipt"}));
        tl1_3.missions.add(new CourseModel.Mission(3, "提出分開結帳 (Split the bill)", "提出分開結帳 (Split the bill)", new String[]{"split", "separately", "separate", "each"}));
        tu1.lessons.add(tl1_3);

        travelTrack.units.add(tu1);

        // Unit 2: Airport & Transportation
        CourseModel.Unit tu2 = new CourseModel.Unit("travel_u2", "travel",
                "Unit 2: Airport & Transit", "第 2 單元：機場出入境與交通問路",
                "Check-in, customs immigration, lost baggage, subway and street directions.",
                "機場報到劃位、海關入境應答、行李遺失申報與街頭問路。");

        // 2.1 Airport Check-in
        CourseModel.Lesson tl2_1 = new CourseModel.Lesson("travel_u2_l1", "travel", "travel_u2",
                "Airport Check-in & Window Seat", "機場報到劃位與更換靠窗座位",
                "Check in your bags and ask the ground staff for a window or aisle seat.",
                "在機場櫃檯辦理登機報到、托運行李並要求靠窗或靠走道座位。",
                "airport",
                "You are an airline check-in ground agent. Greet the passenger, ask for passport, baggage details, and seat preference.");
        tl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Could I have a window seat, please?", "可以給我一個靠窗的座位嗎？", "/kʊd aɪ hæv ə ˈwɪndoʊ siːt pliːz/", "靠窗座位要求"));
        tl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("I have one piece of check-in luggage and one carry-on.", "我有一件托運行李和一件隨身行李。", "/aɪ hæv wʌn piːs ʌv ˈtʃɛk.ɪn ˈlʌɡɪdʒ/", "行李數量說明"));
        tl2_1.missions.add(new CourseModel.Mission(1, "主動出示護照並表明飛往目的地", "主動出示護照並表明飛往目的地", new String[]{"passport", "flying", "flight", "to", "here is"}));
        tl2_1.missions.add(new CourseModel.Mission(2, "說明行李托運數量 (Check-in luggage)", "說明行李托運數量 (Check-in luggage)", new String[]{"luggage", "bag", "bags", "check-in", "suitcase", "piece"}));
        tl2_1.missions.add(new CourseModel.Mission(3, "要求靠窗或靠走道座位 (Window/Aisle seat)", "要求靠窗或靠走道座位 (Window/Aisle seat)", new String[]{"window", "aisle", "seat"}));
        tu2.lessons.add(tl2_1);

        // 2.2 Customs Immigration
        CourseModel.Lesson tl2_2 = new CourseModel.Lesson("travel_u2_l2", "travel", "travel_u2",
                "Customs Immigration & Travel Purpose", "海關入境問答與旅遊目的",
                "Answer customs officer questions clearly about your stay and return ticket.",
                "向海關官員清晰回答入境目的、停留天數與回程機票。",
                "airport",
                "You are an immigration customs officer at border control. Ask the traveler about their visit purpose, duration of stay, and accommodation.");
        tl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("I'm here on vacation for 7 days.", "我是來觀光度假的，預計停留 7 天。", "/aɪm hɪər ɒn veɪˈkeɪʃən fɔːr ˈsɛvən deɪz/", "入境旅遊目的"));
        tl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("I'll be staying at the Hilton Hotel downtown.", "我會住在市區的希爾頓飯店。", "/aɪl biː ˈsteɪɪŋ æt ðə ˈhɪltən hoʊˈtɛl/", "住宿地點交代"));
        tl2_2.missions.add(new CourseModel.Mission(1, "說明入境目的（如觀光旅遊/度假）", "說明入境目的（如觀光旅遊/度假）", new String[]{"vacation", "sightseeing", "holiday", "travel", "tourism", "visit"}));
        tl2_2.missions.add(new CourseModel.Mission(2, "告知停留天數與住宿飯店名稱", "告知停留天數與住宿飯店名稱", new String[]{"stay", "days", "week", "hotel", "staying"}));
        tl2_2.missions.add(new CourseModel.Mission(3, "表明已訂好回程機票 (Return ticket)", "表明已訂好回程機票 (Return ticket)", new String[]{"return", "ticket", "flight back", "leaving"}));
        tu2.lessons.add(tl2_2);

        // 2.3 Subway & Directions
        CourseModel.Lesson tl2_3 = new CourseModel.Lesson("travel_u2_l3", "travel", "travel_u2",
                "Subway & Street Directions", "地鐵購票與街頭問路",
                "Ask pedestrians for directions to the train station or landmark.",
                "向路人詢問地鐵站方向、換乘路線與步行時間。",
                "travel",
                "You are a helpful local pedestrian in London/New York. Guide the traveler to their destination clearly.");
        tl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Excuse me, how do I get to the nearest subway station?", "不好意思，請問最近的地鐵站怎麼走？", "/ɪkˈskjuːz miː, haʊ duː aɪ ɡɛt tuː ðə ˈnɪərɪst ˈsʌbweɪ ˈsteɪʃən/", "問路經典句"));
        tl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Is it within walking distance from here?", "從這裡走路能到嗎？", "/ɪz ɪt wɪˈðɪn ˈwɔːkɪŋ ˈdɪstəns frʌm hɪər/", "確認步行距離"));
        tl2_3.missions.add(new CourseModel.Mission(1, "禮貌詢問前往某地/地鐵站的方向", "禮貌詢問前往某地/地鐵站的方向", new String[]{"how do i get", "where is", "station", "way to", "subway", "metro"}));
        tl2_3.missions.add(new CourseModel.Mission(2, "詢問步行或搭車大約需要多久 (How long)", "詢問步行或搭車大約需要多久 (How long)", new String[]{"how long", "minutes", "walk", "far", "distance"}));
        tl2_3.missions.add(new CourseModel.Mission(3, "致謝並確認方向 (Thank you)", "致謝並確認方向 (Thank you)", new String[]{"thank you", "thanks", "appreciate", "got it"}));
        tu2.lessons.add(tl2_3);

        // 2.4 Lost Baggage Claim
        CourseModel.Lesson tl2_4 = new CourseModel.Lesson("travel_u2_l4", "travel", "travel_u2",
                "Lost Baggage Claim & Help Desk", "機場行李遺失與服務台申報",
                "Report a missing suitcase at the airport baggage service counter.",
                "在行李查詢處申報行李未送達、描述外觀特徵並留下聯繫地址。",
                "airport",
                "You are a helpful baggage claim service staff. Ask the passenger for their baggage claim tag, suitcase color/brand, and delivery address.");
        tl2_4.warmupPhrases.add(new CourseModel.WarmupPhrase("My checked suitcase didn't come out on carousel 4.", "我的托運行李沒有在 4 號轉盤出來。", "/maɪ tʃɛkt ˈsuːt.keɪs ˈdɪdnt kʌm aʊt ɒn ˌkær.əˈsɛl/", "行李遺失說明"));
        tl2_4.warmupPhrases.add(new CourseModel.WarmupPhrase("It's a black hard-shell suitcase with a red tag.", "那是一個黑色硬殼行李箱，上面有紅色行李吊牌。", "/ɪts ə blæk hɑːrd.ʃɛl ˈsuːt.keɪs wɪð ə rɛd tæɡ/", "行李外觀描述"));
        tl2_4.missions.add(new CourseModel.Mission(1, "說明行李未在轉盤出現並出示行李票 (Claim tag)", "說明行李未在轉盤出現並出示行李票 (Claim tag)", new String[]{"baggage", "suitcase", "carousel", "missing", "tag", "didn't come"}));
        tl2_4.missions.add(new CourseModel.Mission(2, "清晰描述行李箱的顏色與外觀特徵", "清晰描述行李箱的顏色與外觀特徵", new String[]{"black", "silver", "blue", "hard-shell", "tag", "brand", "rimowa", "samsonite"}));
        tl2_4.missions.add(new CourseModel.Mission(3, "留下飯店地址與聯絡電話要求送達", "留下飯店地址與聯絡電話要求送達", new String[]{"hotel", "deliver", "phone", "address", "call me"}));
        tu2.lessons.add(tl2_4);

        travelTrack.units.add(tu2);

        // Unit 3: Hotel & Accommodation
        CourseModel.Unit tu3 = new CourseModel.Unit("travel_u3", "travel",
                "Unit 3: Hotel & Accommodation", "第 3 單元：飯店入住與設施要求",
                "Check-in, room problems, luggage storage, and check-out services.",
                "飯店 Check-in、要求高樓層、房間設備問題反映與退房寄存。");

        // 3.1 Hotel Check-in
        CourseModel.Lesson tl3_1 = new CourseModel.Lesson("travel_u3_l1", "travel", "travel_u3",
                "Hotel Check-in & Quiet High Floor", "飯店 Check-in 與要求高樓層安靜房",
                "Check into your hotel room and request a quiet room on a high floor.",
                "辦理飯店入住，確認早餐時間並要求高樓層安靜房間。",
                "hotel",
                "You are a receptionist at a boutique hotel. Welcome the guest, confirm their booking, and assist with room keys.");
        tl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Hi, I have a reservation under the name John Smith.", "嗨，我有一筆訂房，登記的名字是 John Smith。", "/haɪ, aɪ hæv ə ˌrɛzərˈveɪʃən ˈʌndər ðə neɪm dʒɒn smɪθ/", "入住開場句"));
        tl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("If possible, could we get a quiet room on a higher floor?", "如果可以的話，能給我們高樓層安靜一點的房間嗎？", "/ɪf ˈpɒsəbl, kʊd wiː ɡɛt ə ˈkwaɪət ruːm/", "客房特殊要求"));
        tl3_1.missions.add(new CourseModel.Mission(1, "出示預訂並說明入住姓名 (Reservation)", "出示預訂並說明入住姓名 (Reservation)", new String[]{"reservation", "booking", "check in", "name", "booked"}));
        tl3_1.missions.add(new CourseModel.Mission(2, "要求高樓層或安靜房間 (High floor / Quiet)", "要求高樓層或安靜房間 (High floor / Quiet)", new String[]{"high floor", "higher floor", "quiet", "view", "bed"}));
        tl3_1.missions.add(new CourseModel.Mission(3, "詢問 Wi-Fi 密碼或早餐時間 (Wi-Fi / Breakfast)", "詢問 Wi-Fi 密碼或早餐時間 (Wi-Fi / Breakfast)", new String[]{"wifi", "wi-fi", "breakfast", "password", "time"}));
        tu3.lessons.add(tl3_1);

        // 3.2 Room Maintenance
        CourseModel.Lesson tl3_2 = new CourseModel.Lesson("travel_u3_l2", "travel", "travel_u3",
                "Room Maintenance & Requesting Room Change", "房間設備故障與要求換房",
                "Call the front desk to report broken air conditioning or hot water issues.",
                "致電櫃檯反映冷氣故障或沒有熱水，必要時要求換房。",
                "hotel",
                "You are the front desk agent on duty. Handle the guest's complaint with empathy and send assistance promptly.");
        tl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("The air conditioning in room 402 isn't working.", "402 號房的冷氣完全不運轉。", "/ðiː ɛər kənˈdɪʃənɪŋ ɪn ruːm fɔːr oʊ tuː ˈɪznt ˈwɜːrkɪŋ/", "設備故障描述"));
        tl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Could you send someone up or switch us to another room?", "可以派人上來檢查，或是幫我們換房間嗎？", "/kʊd juː sɛnd ˈsʌmwʌn ʌp ɔːr swɪtʃ ʌs tuː əˈnʌðər ruːm/", "要求換房"));
        tl3_2.missions.add(new CourseModel.Mission(1, "表明房號並清楚描述設備故障問題", "表明房號並清楚描述設備故障問題", new String[]{"room", "air conditioning", "ac", "hot water", "working", "broken", "noise"}));
        tl3_2.missions.add(new CourseModel.Mission(2, "要求派維修人員上樓檢查 (Send someone)", "要求派維修人員上樓檢查 (Send someone)", new String[]{"send", "check", "fix", "look", "repair"}));
        tl3_2.missions.add(new CourseModel.Mission(3, "提出若無法修復希望能更換房間 (Switch room)", "提出若無法修復希望能更換房間 (Switch room)", new String[]{"switch", "change", "another room", "different room"}));
        tu3.lessons.add(tl3_2);

        // 3.3 Check-out & Shuttle
        CourseModel.Lesson tl3_3 = new CourseModel.Lesson("travel_u3_l3", "travel", "travel_u3",
                "Hotel Check-out & Luggage Storage", "飯店退房與行李暫存接送",
                "Check out of the hotel, leave bags until flight time, and book a taxi.",
                "辦理退房結清帳單、暫存行李至下午並預約前往機場的計程車。",
                "hotel",
                "You are the front desk cashier. Assist the guest with invoice review, luggage storage tag, and airport taxi reservation.");
        tl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("We'd like to check out of room 508, please.", "我們要辦理 508 號房退房。", "/wiːd laɪk tuː tʃɛk aʊt ʌv ruːm faɪv oʊ eɪt/", "退房開場句"));
        tl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Could we leave our bags here until 4 PM?", "我們可以把行李暫存在這裡直到下午 4 點嗎？", "/kʊd wiː liːv ˈaʊər bæɡz hɪər ʌnˈtɪl fɔːr piː.ɛm/", "行李寄存實用句"));
        tl3_3.missions.add(new CourseModel.Mission(1, "告知房號並辦理退房手續 (Check out)", "告知房號並辦理退房手續 (Check out)", new String[]{"check out", "room", "key", "leaving", "bill"}));
        tl3_3.missions.add(new CourseModel.Mission(2, "要求暫存行李至出發時間 (Leave bags / Storage)", "要求暫存行李至出發時間 (Leave bags / Storage)", new String[]{"leave", "bags", "luggage", "store", "hold", "afternoon"}));
        tl3_3.missions.add(new CourseModel.Mission(3, "請櫃檯幫忙叫計程車或預約接駁 (Call a cab / Taxi)", "請櫃檯幫忙叫計程車或預約接駁 (Call a cab / Taxi)", new String[]{"taxi", "cab", "airport", "shuttle", "call"}));
        tu3.lessons.add(tl3_3);

        travelTrack.units.add(tu3);

        // Unit 4: Shopping & Emergency
        CourseModel.Unit tu4 = new CourseModel.Unit("travel_u4", "travel",
                "Unit 4: Shopping, Tax Refund & Health", "第 4 單元：購物試穿、退稅與藥局求助",
                "Fitting clothes, department store tax refund, and pharmacy symptom descriptions.",
                "服飾專櫃試穿換尺寸、百貨商場退稅手續與藥局購藥症狀描述。");

        // 4.1 Clothing Store Fitting
        CourseModel.Lesson tl4_1 = new CourseModel.Lesson("travel_u4_l1", "travel", "travel_u4",
                "Clothing Store Fitting & Different Sizes", "服飾店試穿與尺寸挑選",
                "Ask the sales assistant for a fitting room and exchange for a larger size.",
                "詢問試衣間位置、試穿外套並請店員拿大一號或不同顏色。",
                "travel",
                "You are an energetic retail fashion sales assistant. Help the shopper find the right fit and suggest colors.");
        tl4_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Could I try this jacket on? Where are the fitting rooms?", "我可以試穿這件外套嗎？請問試衣間在哪裡？", "/kʊd aɪ traɪ ðɪs ˈdʒækɪt ɒn/", "試穿必備句"));
        tl4_1.warmupPhrases.add(new CourseModel.WarmupPhrase("It's a bit tight; do you have this in a medium or large?", "這件有點緊，請問有 M 號或 L 號嗎？", "/ɪts ə bɪt taɪt, du juː hæv ðɪs ɪn ə ˈmiːdiəm/", "換尺寸表達"));
        tl4_1.missions.add(new CourseModel.Mission(1, "詢問試衣間位置並要求試穿 (Try on / Fitting room)", "詢問試衣間位置並要求試穿 (Try on / Fitting room)", new String[]{"try on", "fitting room", "try this", "wear"}));
        tl4_1.missions.add(new CourseModel.Mission(2, "說明尺寸大小並要求更換 (Medium / Large / Small)", "說明尺寸大小並要求更換 (Medium / Large / Small)", new String[]{"size", "medium", "large", "small", "tight", "loose", "bigger"}));
        tl4_1.missions.add(new CourseModel.Mission(3, "確認是否有折扣或詢問價錢 (Discount / Price)", "確認是否有折扣或詢問價錢 (Discount / Price)", new String[]{"discount", "sale", "price", "how much", "take it"}));
        tu4.lessons.add(tl4_1);

        // 4.2 Tax Refund
        CourseModel.Lesson tl4_2 = new CourseModel.Lesson("travel_u4_l2", "travel", "travel_u4",
                "Department Store Tax Refund", "百貨公司辦理購物退稅",
                "Ask customer service for VAT tax refund forms with passport and receipts.",
                "在百貨服務台出示護照與發票，辦理退稅表格填寫與退稅方式選擇。",
                "travel",
                "You are a tax refund officer at a major department store. Check the receipts, passport eligibility, and offer cash or credit card refund.");
        tl4_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Where can I apply for the VAT tax refund?", "請問在哪裡可以辦理退稅手續？", "/wɛər kæn aɪ əˈplaɪ fɔːr ðə væt tæks ˈriːfʌnd/", "詢問退稅處"));
        tl4_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Here are my receipts and passport. I'd prefer refund to my card.", "這是我的發票和護照，我希望退稅退回信用卡。", "/hɪər ɑːr maɪ rɪˈsiːts ænd ˈpɑːspɔːrt/", "出示單據與選擇退稅方式"));
        tl4_2.missions.add(new CourseModel.Mission(1, "出示護照與購物收據辦理退稅 (Passport & Receipts)", "出示護照與購物收據辦理退稅 (Passport & Receipts)", new String[]{"passport", "receipt", "receipts", "tax refund", "vat"}));
        tl4_2.missions.add(new CourseModel.Mission(2, "選擇退款方式（現金退款或信用卡退刷）", "選擇退款方式（現金退款或信用卡退刷）", new String[]{"card", "cash", "credit card", "refund"}));
        tl4_2.missions.add(new CourseModel.Mission(3, "詢問海關蓋章或機場投遞流程 (Customs stamp)", "詢問海關蓋章或機場投遞流程 (Customs stamp)", new String[]{"airport", "customs", "stamp", "envelope", "mail"}));
        tu4.lessons.add(tl4_2);

        // 4.3 Pharmacy & Symptoms
        CourseModel.Lesson tl4_3 = new CourseModel.Lesson("travel_u4_l3", "travel", "travel_u4",
                "Pharmacy Consultation & Describing Symptoms", "藥局購藥與描述身體不適",
                "Explain your symptoms to a pharmacist to get pain relievers or cold medicine.",
                "向藥師描述頭痛、喉嚨痛或腸胃不適，詢問服藥劑量與次數。",
                "travel",
                "You are an empathetic pharmacist. Ask the customer about their symptoms, allergies, and give clear medication instructions.");
        tl4_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I have a sore throat and a bad headache.", "我喉嚨很痛，而且頭痛得很厲害。", "/aɪ hæv ə sɔːr θroʊt ænd ə bæd ˈhɛdeɪk/", "描述症狀"));
        tl4_3.warmupPhrases.add(new CourseModel.WarmupPhrase("How many times a day should I take this medication?", "這個藥一天需要吃幾次？", "/haʊ ˈmɛni taɪmz ə deɪ ʃʊd aɪ teɪk ðɪs ˌmɛdɪˈkeɪʃən/", "詢問用藥劑量"));
        tl4_3.missions.add(new CourseModel.Mission(1, "清楚說明身體不適症狀（如頭痛/喉嚨痛/發燒）", "清楚說明身體不適症狀（如頭痛/喉嚨痛/發燒）", new String[]{"headache", "sore throat", "fever", "stomach", "cough", "cold", "dizzy"}));
        tl4_3.missions.add(new CourseModel.Mission(2, "詢問每日服用次數與是否飯後服用 (After meals)", "詢問每日服用次數與是否飯後服用 (After meals)", new String[]{"how many", "times", "after meals", "take", "dosage", "food"}));
        tl4_3.missions.add(new CourseModel.Mission(3, "確認是否有嗜睡副作用 (Drowsy / Side effects)", "確認是否有嗜睡副作用 (Drowsy / Side effects)", new String[]{"drowsy", "sleepy", "side effect", "driving"}));
        tu4.lessons.add(tl4_3);

        travelTrack.units.add(tu4);
        cachedTracks.add(travelTrack);

        // ════════════════════════════════════════════════════════════════════
        // 💼 Track 2: Business & Career English (9 Lessons, 3 Units)
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track bizTrack = new CourseModel.Track(
                "business", "💼",
                "Business & Career English", "職場商務與跨國溝通",
                "Master remote meetings, project blockers, polite negotiations, and job interviews.",
                "掌握英文會議主持、進度匯報、商業社交接待與外商求職面試談判技巧。"
        );

        // Unit 1: Meetings & Updates
        CourseModel.Unit bu1 = new CourseModel.Unit("biz_u1", "business",
                "Unit 1: Remote Meetings & Updates", "第 1 單元：跨國線上會議與專案進度",
                "Icebreaking in meetings, delivering status updates, and alignment.",
                "線上會議開場破冰、專案進度匯報與排除阻礙。");

        // 1.1 Meeting Icebreaker
        CourseModel.Lesson bl1_1 = new CourseModel.Lesson("biz_u1_l1", "business", "biz_u1",
                "Meeting Icebreaker & Self Introduction", "會議開場破冰與自我介紹",
                "Introduce yourself concisely in an international team standup meeting.",
                "在跨國團隊會議中簡明自我介紹、說明職責與問候同仁。",
                "business",
                "You are chairing a global sync meeting. Ask the new teammate to introduce themselves and their role.");
        bl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Good morning everyone, thrilled to join the team as the tech lead.", "大家早安，很高興以技術主管的身份加入團隊。", "/ɡʊd ˈmɔːrnɪŋ ˈɛvrɪwʌn, θrɪld tuː dʒɔɪn ðə tiːm/", "自我介紹開場"));
        bl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("I'll be focusing on optimizing our mobile backend infrastructure.", "我接下來會主要負責優化我們的手機後端基礎設施。", "/aɪl biː ˈfoʊkəsɪŋ ɒn ˈɒptɪmaɪzɪŋ .../", "說明專案職責"));
        bl1_1.missions.add(new CourseModel.Mission(1, "向與會同仁打招呼並說明自己的角色 (Role)", "向與會同仁打招呼並說明自己的角色 (Role)", new String[]{"morning", "everyone", "role", "lead", "engineer", "designer", "manager", "name is"}));
        bl1_1.missions.add(new CourseModel.Mission(2, "簡要提及本季或近期負責的主要目標", "簡要提及本季或近期負責的主要目標", new String[]{"responsible", "focus", "working on", "goal", "project", "building"}));
        bl1_1.missions.add(new CourseModel.Mission(3, "表達期待與團隊合作 (Look forward to)", "表達期待與團隊合作 (Look forward to)", new String[]{"look forward", "working together", "collaborate", "excited", "happy to be here"}));
        bu1.lessons.add(bl1_1);

        // 1.2 Status Updates & Blockers
        CourseModel.Lesson bl1_2 = new CourseModel.Lesson("biz_u1_l2", "business", "biz_u1",
                "Project Status Updates & Blockers", "專案進度匯報與遭遇阻礙",
                "Give a crisp 2-minute status update on what's done and current blockers.",
                "清晰匯報已完成事項、進行中項目與需要團隊協助排除的阻礙 (Blockers)。",
                "business",
                "You are an agile project manager. Ask the engineer about their sprint progress and any blockers.");
        bl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("We've completed the API migration; currently we're testing checkout.", "我們已經完成了 API 遷移，目前正在測試結帳流程。", "/wiːv kəmˈpliːtɪd ðiː ˌeɪ.piːˈaɪ maɪˈɡreɪʃən/", "進度匯報句型"));
        bl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("We're blocked by the third-party auth service and need a quick sync.", "我們目前卡在第三方驗證服務，需要快速同步對齊一下。", "/wɪər blɒkt baɪ ðə θɜːrd ˈpɑːrti ˌɔːθ ˈsɜːrvɪs/", "提出阻礙 (Blockers)"));
        bl1_2.missions.add(new CourseModel.Mission(1, "說明最近已完成的里程碑 (Completed)", "說明最近已完成的里程碑 (Completed)", new String[]{"completed", "finished", "shipped", "done", "launched"}));
        bl1_2.missions.add(new CourseModel.Mission(2, "說明目前進行中的項目 (Currently working on)", "說明目前進行中的項目 (Currently working on)", new String[]{"currently", "working on", "testing", "building", "developing"}));
        bl1_2.missions.add(new CourseModel.Mission(3, "提出阻礙並尋求跨團隊支援 (Blocker / Need help)", "提出阻礙並尋求跨團隊支援 (Blocker / Need help)", new String[]{"blocker", "blocked", "need help", "sync", "support", "dependency"}));
        bu1.lessons.add(bl1_2);

        // 1.3 Disagreeing Politely & Consensus
        CourseModel.Lesson bl1_3 = new CourseModel.Lesson("biz_u1_l3", "business", "biz_u1",
                "Disagreeing Politely & Reaching Consensus", "委婉表達異議與尋求共識",
                "Express reservations politely and propose an alternative compromise.",
                "在跨國討論中委婉提出疑慮、給出替代方案並達成共識。",
                "business",
                "You are a product director proposing a tight launch deadline. Listen to the engineer's concerns and find middle ground.");
        bl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I see your point, but I have some concerns regarding the timeline.", "我理解您的考量，但我對時程表有些疑慮。", "/aɪ siː jʊər pɔɪnt, bʌt aɪ hæv sʌm kənˈsɜːrnz/", "委婉表達疑慮"));
        bl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("What if we phase the rollout into two releases instead?", "如果我們把發布拆成兩個階段分批上線，您覺得如何？", "/wʌt ɪf wiː feɪz ðə ˈroʊl.aʊt ˈɪntuː tuː rɪˈliːsɪz/", "提出折衷方案"));
        bl1_3.missions.add(new CourseModel.Mission(1, "肯定對方觀點並委婉切入疑慮 (I see your point, but...)", "肯定對方觀點並委婉切入疑慮 (I see your point, but...)", new String[]{"see your point", "understand", "concern", "risk", "however", "worry"}));
        bl1_3.missions.add(new CourseModel.Mission(2, "提出具體替代方案或階段性發布 (Alternative / Phased)", "提出具體替代方案或階段性發布 (Alternative / Phased)", new String[]{"what if", "suggest", "alternative", "phase", "step", "how about"}));
        bl1_3.missions.add(new CourseModel.Mission(3, "確認下一步行動與負責人 (Next steps / Align)", "確認下一步行動與負責人 (Next steps / Align)", new String[]{"next step", "align", "agreed", "action item", "follow up"}));
        bu1.lessons.add(bl1_3);

        bizTrack.units.add(bu1);

        // Unit 2: Networking & Client Hosting
        CourseModel.Unit bu2 = new CourseModel.Unit("biz_u2", "business",
                "Unit 2: Networking & Client Hosting", "第 2 單元：商業社交與海外客戶接待",
                "Trade show networking, card exchange, office tours, and business dinners.",
                "國際展會交流、互換名片、接待參觀辦公室與商務晚宴祝酒。");

        // 2.1 Trade Show Networking
        CourseModel.Lesson bl2_1 = new CourseModel.Lesson("biz_u2_l1", "business", "biz_u2",
                "Trade Show Networking & Exchanging Contacts", "展會社交與互換商業名片",
                "Start a conversation at a tech booth, introduce products, and exchange LinkedIn/cards.",
                "在展覽攤位自然攀談、介紹公司產品亮點並交換名片與 LinkedIn。",
                "business",
                "You are an exhibitor at a tech conference. Welcome the booth visitor, discuss industry trends, and swap business contacts.");
        bl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Hi, what brought you to the conference this year?", "嗨！今年是什麼主題吸引您來參加這場研討會？", "/haɪ, wʌt brɔːt juː tuː ðə ˈkɒnfərəns/", "展會破冰句"));
        bl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Here's my card, let's definitely connect on LinkedIn.", "這是我的名片，我們一定要在 LinkedIn 上保持聯繫！", "/hɪərz maɪ kɑːrd, lɛts ˈdɛfɪnɪtli kəˈnɛkt/", "交換聯繫方式"));
        bl2_1.missions.add(new CourseModel.Mission(1, "主動向展位人員打招呼並詢問產品特色", "主動向展位人員打招呼並詢問產品特色", new String[]{"booth", "product", "demo", "features", "solutions", "interesting"}));
        bl2_1.missions.add(new CourseModel.Mission(2, "介紹自己的公司與正在尋找的合作機會", "介紹自己的公司與正在尋找的合作機會", new String[]{"company", "looking for", "partnership", "integrate", "collaborate", "we provide"}));
        bl2_1.missions.add(new CourseModel.Mission(3, "提議互換名片或交換聯繫方式 (Exchange cards / LinkedIn)", "提議互換名片或交換聯繫方式 (Exchange cards / LinkedIn)", new String[]{"card", "linkedin", "contact", "email", "keep in touch", "follow up"}));
        bu2.lessons.add(bl2_1);

        // 2.2 Client Hosting & Office Tour
        CourseModel.Lesson bl2_2 = new CourseModel.Lesson("biz_u2_l2", "business", "biz_u2",
                "Hosting Overseas Clients & Office Tour", "接待海外客戶與參觀辦公室",
                "Welcome a visiting foreign partner, offer drinks, and show them around.",
                "在公司前台熱情迎接外賓、提供咖啡茶飲並帶領參觀研發中心。",
                "business",
                "You are a foreign business partner visiting the Taipei office for the first time. Express appreciation for the warm hospitality.");
        bl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Welcome to our office! How was your flight into Taipei?", "歡迎來到我們辦公室！飛往台北的航班還順利嗎？", "/ˈwɛlkəm tuː ˈaʊər ˈɒfɪs! haʊ wʌz jʊər flaɪt/", "接待寒暄開場"));
        bl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Can I get you some water, coffee, or hot tea?", "需要為您準備水、咖啡還是熱茶嗎？", "/kæn aɪ ɡɛt juː sʌm ˈwɔːtər ˈkɒfi ɔːr hɒt tiː/", "招待飲品"));
        bl2_2.missions.add(new CourseModel.Mission(1, "熱情迎接客戶並關心旅途與時差 (Flight / Jet lag)", "熱情迎接客戶並關心旅途與時差 (Flight / Jet lag)", new String[]{"welcome", "flight", "trip", "jet lag", "taiwan", "taipei", "how was"}));
        bl2_2.missions.add(new CourseModel.Mission(2, "主動招待茶水或咖啡 (Coffee / Tea / Water)", "主動招待茶水或咖啡 (Coffee / Tea / Water)", new String[]{"coffee", "tea", "water", "drink", "get you", "comfortable"}));
        bl2_2.missions.add(new CourseModel.Mission(3, "引導前往會議室並概述今日議程 (Agenda / Meeting room)", "引導前往會議室並概述今日議程 (Agenda / Meeting room)", new String[]{"conference room", "meeting room", "agenda", "start", "presentation", "this way"}));
        bu2.lessons.add(bl2_2);

        // 2.3 Business Dinner
        CourseModel.Lesson bl2_3 = new CourseModel.Lesson("biz_u2_l3", "business", "biz_u2",
                "Business Dinner Banter & Toasts", "商務晚宴寒暄與祝酒致詞",
                "Keep casual conversation flowing over dinner and propose a polite toast.",
                "在商務晚宴上自然開啟輕鬆話題、介紹在地特色並舉杯祝酒慶祝合作。",
                "business",
                "You are a foreign client enjoying dinner at a fine restaurant. Share enthusiasm about the joint partnership and local culture.");
        bl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I'd like to propose a toast to our successful partnership!", "我想提議舉杯，祝我們的合作圓滿成功！", "/aɪd laɪk tuː prəˈpoʊz ə toʊst tuː ˈaʊər səkˈsɛsfʊl ˈpɑːrtnərʃɪp/", "商務祝酒"));
        bl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Have you had a chance to try our local beef noodles yet?", "您有機會品嚐過我們在地的牛肉麵了嗎？", "/hæv juː hæd ə tʃæns tuː traɪ ˈaʊər ˈloʊkəl biːf ˈnuːdlz/", "在地美食話題"));
        bl2_3.missions.add(new CourseModel.Mission(1, "介紹一道在地經典特色美食給客戶", "介紹一道在地經典特色美食給客戶", new String[]{"dish", "food", "beef noodle", "dumpling", "night market", "specialty", "try"}));
        bl2_3.missions.add(new CourseModel.Mission(2, "舉杯祝酒慶祝團隊與未來的合作 (Propose a toast / Cheers)", "舉杯祝酒慶祝團隊與未來的合作 (Propose a toast / Cheers)", new String[]{"toast", "cheers", "partnership", "success", "future", "raise a glass"}));
        bl2_3.missions.add(new CourseModel.Mission(3, "得體結尾並表達對明天行程的期待", "得體結尾並表達對明天行程的期待", new String[]{"tomorrow", "wonderful evening", "thank you", "great dinner", "rest well"}));
        bu2.lessons.add(bl2_3);

        bizTrack.units.add(bu2);

        // Unit 3: Job Interviews & Negotiation
        CourseModel.Unit bu3 = new CourseModel.Unit("biz_u3", "business",
                "Unit 3: Job Interviews & Negotiations", "第 3 單元：英文求職面試與薪資談判",
                "Self-intro pitch, behavioral STAR questions, and salary/offer negotiations.",
                "外商面試一分鐘自我介紹、行為面試 STAR 原則與薪資福利協商。");

        // 3.1 Self Intro Pitch
        CourseModel.Lesson bl3_1 = new CourseModel.Lesson("biz_u3_l1", "business", "biz_u3",
                "Interview Self-Intro & Core Strengths", "英文面試開場與核心優勢陳述",
                "Deliver a structured 90-second pitch covering experience, impact, and fit.",
                "自信闡述過往關鍵專案戰績、專業優勢以及與該職位的契合度。",
                "interview",
                "You are an executive interviewer at a global tech firm. Ask the candidate: 'Tell me about yourself and why you are interested in this position.'");
        bl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Over the past 5 years, I've specialized in scaling cloud systems.", "在過去 5 年中，我專注於大規模雲端架構的擴展。", "/ˈoʊvər ðə pæst faɪv jɪərz, aɪv ˈspɛʃəlaɪzd ɪn.../", "經驗亮點概括"));
        bl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("What excites me about this role is your team's focus on AI innovation.", "這份職位最吸引我的是貴團隊對 AI 創新的專注投入。", "/wʌt ɪkˈsaɪts miː əˈbaʊt ðɪs roʊl ɪz.../", "表達應徵動機"));
        bl3_1.missions.add(new CourseModel.Mission(1, "概述過往經歷與核心專業領域 (Experience & Background)", "概述過往經歷與核心專業領域 (Experience & Background)", new String[]{"years", "experience", "background", "specialized", "engineer", "designer", "built"}));
        bl3_1.missions.add(new CourseModel.Mission(2, "具體量化一項過往成果 (Numbers / Growth / Impact)", "具體量化一項過往成果 (Numbers / Growth / Impact)", new String[]{"percent", "%", "users", "reduced", "improved", "increased", "revenue", "scale"}));
        bl3_1.missions.add(new CourseModel.Mission(3, "說明為何對本公司/職位感興趣 (Why this company)", "說明為何對本公司/職位感興趣 (Why this company)", new String[]{"excited", "admire", "mission", "fit", "opportunity", "growth", "why i want"}));
        bu3.lessons.add(bl3_1);

        // 3.2 STAR Behavioral Method
        CourseModel.Lesson bl3_2 = new CourseModel.Lesson("biz_u3_l2", "business", "biz_u3",
                "Behavioral Questions with STAR Method", "行為面試 STAR 原則實戰回答",
                "Answer a challenging conflict or failure question with Situation, Task, Action, Result.",
                "運用 STAR 結構回答「描述一次與同事意見分歧或專案挑戰」的經典考題。",
                "interview",
                "You are a hiring manager asking: 'Tell me about a time you faced a major technical challenge or deadline crunch.'");
        bl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("When our database had a major outage, I stepped in to lead the triage.", "當資料庫發生重大故障時，我主動介入帶領團隊進行排查。", "/wɛn ˈaʊər ˈdeɪtəbeɪs hæd ə ˈmeɪdʒər ˈaʊtɪdʒ/", "Situation 交代背景"));
        bl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("As a result, we restored service within 30 minutes with zero data loss.", "最終，我們在 30 分鐘內恢復服務且零資料遺失。", "/æz ə rɪˈzʌlt, wiː rɪˈstɔːrd ˈsɜːrvɪs/", "Result 成果收尾"));
        bl3_2.missions.add(new CourseModel.Mission(1, "交代具體挑戰背景與目標任務 (Situation & Task)", "交代具體挑戰背景與目標任務 (Situation & Task)", new String[]{"situation", "faced", "challenge", "deadline", "task", "problem", "when"}));
        bl3_2.missions.add(new CourseModel.Mission(2, "清晰描述自己採取的行動策略 (Action taken)", "清晰描述自己採取的行動策略 (Action taken)", new String[]{"i decided", "action", "communicated", "implemented", "solved", "led", "step"}));
        bl3_2.missions.add(new CourseModel.Mission(3, "總結最終帶來的具體成效與學習 (Result & Takeaway)", "總結最終帶來的具體成效與學習 (Result & Takeaway)", new String[]{"result", "outcome", "learned", "delivered", "success", "improved", "eventually"}));
        bu3.lessons.add(bl3_2);

        // 3.3 Salary & Offer Negotiation
        CourseModel.Lesson bl3_3 = new CourseModel.Lesson("biz_u3_l3", "business", "biz_u3",
                "Salary & Offer Package Negotiation", "薪資待遇與 Offer 條件協商",
                "Negotiate base pay, remote flexibility, and stock options diplomatically.",
                "禮貌詢問薪資結構、爭取符合市場行情的底薪、遠端彈性與股票期權。",
                "interview",
                "You are a recruiter extending a job offer. Discuss the total compensation package with the candidate.");
        bl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Based on my track record and market rate, I was targeting around $120k.", "根據我的過往戰績與市場行情，我的目標薪資約為 12 萬美元。", "/beɪst ɒn maɪ træk ˈrɛk.ɔːrd.../", "提出期望薪資"));
        bl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Is there any flexibility around stock options or remote work days?", "在股票期權或遠端工作天數方面有協商彈性嗎？", "/ɪz ðɛər ˈɛni ˌflɛksəˈbɪlɪti əˈraʊnd.../", "詢問福利彈性"));
        bl3_3.missions.add(new CourseModel.Mission(1, "表達對 Offer 的感謝與對職位的熱情 (Appreciate the offer)", "表達對 Offer 的感謝與對職位的熱情 (Appreciate the offer)", new String[]{"thank you", "excited", "offer", "appreciate", "grateful", "love to join"}));
        bl3_3.missions.add(new CourseModel.Mission(2, "委婉提出期望的薪酬區間或待遇要求 (Target compensation)", "委婉提出期望的薪酬區間或待遇要求 (Target compensation)", new String[]{"salary", "compensation", "target", "market rate", "package", "base pay"}));
        bl3_3.missions.add(new CourseModel.Mission(3, "確認回覆期限與到職日安排 (Start date / Timeline)", "確認回覆期限與到職日安排 (Start date / Timeline)", new String[]{"start date", "notice period", "decision", "timeline", "review", "sign"}));
        bu3.lessons.add(bl3_3);

        bizTrack.units.add(bu3);
        cachedTracks.add(bizTrack);

        // ════════════════════════════════════════════════════════════════════
        // ☕ Track 3: Daily Conversation & Small Talk (9 Lessons, 3 Units)
        // ════════════════════════════════════════════════════════════════════
        CourseModel.Track dailyTrack = new CourseModel.Track(
                "daily", "☕",
                "Daily Conversation & Small Talk", "日常社交與深度閒聊",
                "Talk about weekends, movies, food, pets, travel stories, and AI trends.",
                "聊週末計畫、熱門影集、毛小孩寵物、旅遊故事與 AI 科技生活趨勢。"
        );

        // Unit 1: Weekend Life & Hobbies
        CourseModel.Unit du1 = new CourseModel.Unit("daily_u1", "daily",
                "Unit 1: Weekend Life & Hobbies", "第 1 單元：週末生活與興趣話題",
                "Weekend outdoor activities, movie/food recommendations, and declining plans.",
                "分享週末行程、探店美食影集推薦與改期邀約。");

        // 1.1 Weekend Hobbies
        CourseModel.Lesson dl1_1 = new CourseModel.Lesson("daily_u1_l1", "daily", "daily_u1",
                "Weekend Plans & Outdoor Hobbies", "週末活動與戶外休閒展開",
                "Chat casually with a friend about outdoor hiking, cafes, or relaxing plans.",
                "和朋友輕鬆聊聊週末的登山健行、探店咖啡廳或放鬆規劃。",
                "daily",
                "You are an energetic and friendly friend. Ask what your buddy did last weekend and share excitement about outdoor activities.");
        dl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("I went hiking in the mountains; the weather was phenomenal!", "我去了山裡爬山，天氣超級棒！", "/aɪ wɛnt ˈhaɪkɪŋ ɪn ðə ˈmaʊntɪnz/", "分享週末行程"));
        dl1_1.warmupPhrases.add(new CourseModel.WarmupPhrase("What did you get up to over the weekend?", "你週末都做了些什麼好玩的？", "/wʌt dɪd juː ɡɛt ʌp tuː ˈoʊvər ðə ˈwiːk.ɛnd/", "反問對方"));
        dl1_1.missions.add(new CourseModel.Mission(1, "分享自己上週末做了一件有趣的事", "分享自己上週末做了一件有趣的事", new String[]{"weekend", "went", "hiking", "movie", "cafe", "cooked", "played", "stayed"}));
        dl1_1.missions.add(new CourseModel.Mission(2, "主動反問對方的週末或近期生活 (How about you?)", "主動反問對方的週末或近期生活 (How about you?)", new String[]{"how about you", "what about you", "did you", "how was your"}));
        dl1_1.missions.add(new CourseModel.Mission(3, "約定下次一起參與某個活動 (Let's do that)", "約定下次一起參與某個活動 (Let's do that)", new String[]{"next time", "let's", "join", "together", "should do that", "sounds fun"}));
        du1.lessons.add(dl1_1);

        // 1.2 Food & Netflix Series
        CourseModel.Lesson dl1_2 = new CourseModel.Lesson("daily_u1_l2", "daily", "daily_u1",
                "Food Spots & Netflix Binge-Watching", "美食探店與熱門影集討論",
                "Recommend a hidden gem restaurant and discuss favorite plot twists.",
                "推薦一家巷弄排隊美食，並與朋友聊聊最近在追的 Netflix 燒腦影集。",
                "daily",
                "You are a movie and foodie enthusiast. Ask for food recommendations and share your current binge-worthy show.");
        dl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Have you checked out that new Italian bistro downtown?", "你有去過市區那家新開的義大利小餐館了嗎？", "/hæv juː tʃɛkt aʊt ðæt njuː ɪˈtæliən ˈbiːstroʊ/", "美食推薦問句"));
        dl1_2.warmupPhrases.add(new CourseModel.WarmupPhrase("I'm totally hooked on this sci-fi thriller series on Netflix!", "我最近完全沉迷在 Netflix 上的一部科幻懸疑影集！", "/aɪm ˈtoʊtəli hʊkt ɒn ðɪs ˈsaɪ.faɪ ˈθrɪlər/", "推薦追劇"));
        dl1_2.missions.add(new CourseModel.Mission(1, "推薦一道喜愛的美食或一家餐廳 (Recommend food)", "推薦一道喜愛的美食或一家餐廳 (Recommend food)", new String[]{"restaurant", "food", "delicious", "pasta", "tacos", "curry", "tried", "recommend"}));
        dl1_2.missions.add(new CourseModel.Mission(2, "分享正在觀看或喜愛的一部電影/影集 (Show / Movie)", "分享正在觀看或喜愛的一部電影/影集 (Show / Movie)", new String[]{"netflix", "movie", "show", "series", "watching", "season", "actor", "plot"}));
        dl1_2.missions.add(new CourseModel.Mission(3, "表達自己對劇情的看法或推薦理由 (Hooked / Worth watching)", "表達自己對劇情的看法或推薦理由 (Hooked / Worth watching)", new String[]{"worth", "hooked", "amazing", "twist", "ending", "recommend", "great"}));
        du1.lessons.add(dl1_2);

        // 1.3 Declining Politely
        CourseModel.Lesson dl1_3 = new CourseModel.Lesson("daily_u1_l3", "daily", "daily_u1",
                "Declining Invitations Politely & Rain Checks", "委婉拒絕邀約與另約時間",
                "Politely turn down a dinner party due to a prior commitment and offer another date.",
                "因已有安排而委婉拒絕朋友的聚餐邀約，並主動提出改約下週。",
                "daily",
                "You are an understanding friend inviting the student to a weekend BBQ party. Handle their rescheduling warmly.");
        dl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I'd love to, but I've already committed to a family dinner.", "我很想去，但我那天已經答應跟家人吃晚餐了。", "/aɪd lʌv tuː, bʌt aɪv ɔːlˈrɛdi kəˈmɪtɪd tuː.../", "委婉拒絕第一步"));
        dl1_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Can I take a rain check and make it up to you next Friday?", "我能先欠你一次、改約下週五補請你嗎？", "/kæn aɪ teɪk ə reɪn tʃɛk.../", "改期必備：Take a rain check"));
        dl1_3.missions.add(new CourseModel.Mission(1, "感謝對方的邀約並委婉說明無法參加的原因", "感謝對方的邀約並委婉說明無法參加的原因", new String[]{"thank you", "invitation", "love to", "can't make it", "prior", "busy", "committed"}));
        dl1_3.missions.add(new CourseModel.Mission(2, "使用 Rain check 或提議另擇日期 (Rain check / Next week)", "使用 Rain check 或提議另擇日期 (Rain check / Next week)", new String[]{"rain check", "next week", "friday", "saturday", "another time", "reschedule"}));
        dl1_3.missions.add(new CourseModel.Mission(3, "祝對方活動玩得愉快 (Have a great time)", "祝對方活動玩得愉快 (Have a great time)", new String[]{"have fun", "great time", "enjoy", "send photos", "catch up"}));
        du1.lessons.add(dl1_3);

        dailyTrack.units.add(du1);

        // Unit 2: Emotional & Deeper Connections
        CourseModel.Unit du2 = new CourseModel.Unit("daily_u2", "daily",
                "Unit 2: Pets, Travel Stories & Wellness", "第 2 單元：毛小孩、旅行奇遇與生活調適",
                "Sharing stories about pets, unforgettable travel, and managing stress.",
                "分享毛小孩趣事、難忘的異國旅行奇遇與紓壓生活哲學。");

        // 2.1 Pets & Animals
        CourseModel.Lesson dl2_1 = new CourseModel.Lesson("daily_u2_l1", "daily", "daily_u2",
                "Talking About Pets & Funny Animal Moments", "聊毛小孩與寵物搞笑日常",
                "Talk about your dog/cat's funny quirks and daily companionship.",
                "分享家中貓狗的可愛怪癖、拆家日常以及寵物帶來的陪伴與治癒感。",
                "daily",
                "You are a pet lover with two playful golden retrievers. Swap funny pet stories enthusiastically.");
        dl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("My golden retriever always greets me at the door with his favorite toy.", "我的黃金獵犬每次都在門口咬著他最愛的玩具迎接我。", "/maɪ ˈɡoʊldən rɪˈtriːvər.../", "描述寵物行為"));
        dl2_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Are you more of a dog person or a cat person?", "你比較是愛狗派還是愛貓派？", "/ɑːr juː mɔːr ʌv ə dɒɡ ˈpɜːrsən ɔːr ə kæt ˈpɜːrsən/", "社交經典破冰問句"));
        dl2_1.missions.add(new CourseModel.Mission(1, "分享自己養的寵物或喜愛的動物類型", "分享自己養的寵物或喜愛的動物類型", new String[]{"dog", "cat", "pet", "puppy", "kitten", "animal", "breed"}));
        dl2_1.missions.add(new CourseModel.Mission(2, "生動描述寵物的一件搞笑或暖心事蹟", "生動描述寵物的一件搞笑或暖心事蹟", new String[]{"funny", "cute", "sleeps", "barks", "toy", "walk", "cuddle", "habit"}));
        dl2_1.missions.add(new CourseModel.Mission(3, "詢問對方的寵物經驗 (Dog or cat person)", "詢問對方的寵物經驗 (Dog or cat person)", new String[]{"do you have", "dog person", "cat person", "how about you", "ever had"}));
        du2.lessons.add(dl2_1);

        // 2.2 Unforgettable Travel
        CourseModel.Lesson dl2_2 = new CourseModel.Lesson("daily_u2_l2", "daily", "daily_u2",
                "Memorable Travel Adventures & Hidden Gems", "難忘的旅行冒險與在地私房景點",
                "Share an unexpected cultural adventure or breathtaking scenery from past trips.",
                "分享一次難忘的異國自由行奇遇、壯麗自然景觀與文化震撼感受。",
                "travel",
                "You are an avid backpacker who has visited over 30 countries. Share cultural insights and travel highlights.");
        dl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("The most memorable part of Japan was stumbling upon a hidden onsen in Kyoto.", "日本最讓我難忘的是在京都意外走進一家隱秘溫泉。", "/ðə moʊst ˈmɛmərəbl pɑːrt.../", "旅遊記憶點"));
        dl2_2.warmupPhrases.add(new CourseModel.WarmupPhrase("The local hospitality truly blew me away.", "當地居民的熱情好客真的讓我無比感動。", "/ðə ˈloʊkəl ˌhɒspɪˈtælɪti ˈtruːli bluː miː əˈweɪ/", "表達文化震撼"));
        dl2_2.missions.add(new CourseModel.Mission(1, "提到一個去過最喜歡的國家或城市 (City / Country)", "提到一個去過最喜歡的國家或城市 (City / Country)", new String[]{"japan", "kyoto", "tokyo", "europe", "italy", "paris", "london", "trip", "visited"}));
        dl2_2.missions.add(new CourseModel.Mission(2, "描述一個印象深刻的景色或意外收穫 (Breathtaking / Culture)", "描述一個印象深刻的景色或意外收穫 (Breathtaking / Culture)", new String[]{"scenery", "mountain", "breathtaking", "culture", "food", "people", "amazing"}));
        dl2_2.missions.add(new CourseModel.Mission(3, "分享下一個 Bucket List 夢想旅遊清單", "分享下一個 Bucket List 夢想旅遊清單", new String[]{"next", "bucket list", "want to go", "iceland", "spain", "hope to"}));
        du2.lessons.add(dl2_2);

        // 2.3 Work-Life Balance
        CourseModel.Lesson dl2_3 = new CourseModel.Lesson("daily_u2_l3", "daily", "daily_u2",
                "Work-Life Balance & De-Stressing Habits", "工作生活平衡與日常紓壓秘訣",
                "Discuss modern burnout and practical habits like digital detox or yoga.",
                "聊聊上班族的壓力管理、週末數位排毒 (Digital Detox) 與冥想運動放鬆法。",
                "daily",
                "You are a supportive friend who practices mindfulness and pilates. Chat about healthy boundaries and unwinding after work.");
        dl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I try to turn off work notifications after 7 PM to set clear boundaries.", "我試著在晚上 7 點後關閉工作通知，建立清楚的生活邊界。", "/aɪ traɪ tuː tɜːrn ɒf.../", "建立生活邊界"));
        dl2_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Meditation and weightlifting help me clear my head after a long day.", "冥想和重訓能幫我在忙碌一天後徹底放空思緒。", "/ˌmɛdɪˈteɪʃən ænd ˈweɪtˌlɪftɪŋ hɛlp miː.../", "放鬆習慣說明"));
        dl2_3.missions.add(new CourseModel.Mission(1, "分享自己近期感到疲倦或忙碌的真實感受", "分享自己近期感到疲倦或忙碌的真實感受", new String[]{"busy", "stress", "tired", "work", "hours", "exhausted", "burnout"}));
        dl2_3.missions.add(new CourseModel.Mission(2, "介紹一種自己最有效的放鬆或減壓方式 (Unwind / Exercise)", "介紹一種自己最有效的放鬆或減壓方式 (Unwind / Exercise)", new String[]{"gym", "run", "yoga", "read", "sleep", "walk", "unwind", "detox"}));
        dl2_3.missions.add(new CourseModel.Mission(3, "彼此打氣並提出一個健康的小目標 (Take it easy)", "彼此打氣並提出一個健康的小目標 (Take it easy)", new String[]{"take it easy", "rest", "healthy", "boundary", "weekend", "cheer"}));
        du2.lessons.add(dl2_3);

        dailyTrack.units.add(du2);

        // Unit 3: Tech & Modern Trends
        CourseModel.Unit du3 = new CourseModel.Unit("daily_u3", "daily",
                "Unit 3: AI & Modern Lifestyle Trends", "第 3 單元：AI 科技、音樂與生活新趨勢",
                "Discussing generative AI, concert experiences, and fitness trends.",
                "探討生成式 AI 對生活的改變、演唱會熱血體驗與健身飲食新潮流。");

        // 3.1 AI & Future Tools
        CourseModel.Lesson dl3_1 = new CourseModel.Lesson("daily_u3_l1", "daily", "daily_u3",
                "AI Assistants & How Tech Changes Daily Life", "AI 智慧工具與未來科技生活",
                "Share how you use AI for coding or language learning and debate future trends.",
                "聊聊自己如何使用 AI 輔助寫程式或學英文，並交流對未來科技的看法。",
                "daily",
                "You are an enthusiastic tech blogger who uses generative AI daily. Discuss coolest AI tools and creative workflows.");
        dl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("I use AI daily to brainstorm ideas and refine my English writing.", "我每天都用 AI 來腦力激盪點子和潤飾英文寫作。", "/aɪ juːz eɪ.aɪ ˈdeɪli tuː.../", "AI 使用場景"));
        dl3_1.warmupPhrases.add(new CourseModel.WarmupPhrase("Do you think AI will replace routine tasks or augment human creativity?", "你認為 AI 會取代常規工作，還是增強人類的創造力？", "/du juː θɪŋk eɪ.aɪ wɪl rɪˈpleɪs.../", "探討未來趨勢"));
        dl3_1.missions.add(new CourseModel.Mission(1, "分享自己日常使用的 AI 或效率軟體工具", "分享自己日常使用的 AI 或效率軟體工具", new String[]{"ai", "chatgpt", "gemini", "tool", "app", "code", "write", "search"}));
        dl3_1.missions.add(new CourseModel.Mission(2, "探討 AI 對工作或語言學習帶來的改變 (Productivity / Learning)", "探討 AI 對工作或語言學習帶來的改變 (Productivity / Learning)", new String[]{"fast", "productive", "learn", "english", "helpful", "save time", "automate"}));
        dl3_1.missions.add(new CourseModel.Mission(3, "表達對未來科技的期待或省思 (Future / Exciting)", "表達對未來科技的期待或省思 (Future / Exciting)", new String[]{"future", "exciting", "potential", "human", "creativity", "change"}));
        du3.lessons.add(dl3_1);

        // 3.2 Concerts & Music
        CourseModel.Lesson dl3_2 = new CourseModel.Lesson("daily_u3_l2", "daily", "daily_u3",
                "Live Concerts & Favorite Music Genres", "演唱會現場與喜愛音樂曲風",
                "Talk about an electrifying live concert experience and your all-time top artists.",
                "分享一次讓人起雞皮疙瘩的演唱會現場體驗，以及最喜歡的樂團歌手。",
                "daily",
                "You are an indie music fanatic who loves attending music festivals. Trade concert memories and artist favorites.");
        dl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("The energy of the crowd at the Coldplay concert was electrifying!", "Coldplay 演唱會全場觀眾的熱情能量真的令人無比震撼！", "/ðiː ˈɛnərdʒi ʌv ðə kraʊd wʌz ɪˈlɛktrɪfaɪ.ɪŋ/", "描述演唱會現場"));
        dl3_2.warmupPhrases.add(new CourseModel.WarmupPhrase("Their live performance gave me absolute goosebumps.", "他們的現場演出真的讓我全身起雞皮疙瘩。", "/ðɛər laɪv pərˈfɔːrməns ɡeɪv miː ˈæbsəluːt ˈɡuːsbʌmps/", "讚嘆現場感染力"));
        dl3_2.missions.add(new CourseModel.Mission(1, "分享自己最喜歡的音樂曲風或歌手樂團", "分享自己最喜歡的音樂曲風或歌手樂團", new String[]{"band", "singer", "pop", "rock", "jazz", "hip hop", "artist", "music"}));
        dl3_2.missions.add(new CourseModel.Mission(2, "描述一次難忘的 Live 演唱會或音樂節現場 (Concert / Live)", "描述一次難忘的 Live 演唱會或音樂節現場 (Concert / Live)", new String[]{"concert", "live", "crowd", "stage", "festival", "energy", "songs"}));
        dl3_2.missions.add(new CourseModel.Mission(3, "詢問對方最近是否有想看的演出 (Upcoming tour)", "詢問對方最近是否有想看的演出 (Upcoming tour)", new String[]{"ticket", "tour", "next concert", "see them", "spotify", "playlist"}));
        du3.lessons.add(dl3_2);

        // 3.3 Fitness & Healthy Habits
        CourseModel.Lesson dl3_3 = new CourseModel.Lesson("daily_u3_l3", "daily", "daily_u3",
                "Fitness Goals, Gym Routines & Healthy Eating", "健身目標、重訓與健康飲食",
                "Swap workout routines, protein goals, and staying motivated consistently.",
                "交流每週健身課表、高蛋白飲食心得以及如何維持規律自律。",
                "daily",
                "You are an energetic certified personal trainer. Share practical workout tips and celebrate fitness milestones.");
        dl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("I hit the gym 4 times a week, focusing on strength training.", "我每週去健身房 4 次，主要以肌力重訓為主。", "/aɪ hɪt ðə dʒɪm fɔːr taɪmz ə wiːk.../", "分享健身習慣"));
        dl3_3.warmupPhrases.add(new CourseModel.WarmupPhrase("Consistency is key; I prioritize getting enough protein and sleep.", "堅持是關鍵，我最看重攝取足夠的蛋白質與優質睡眠。", "/kənˈsɪstənsi ɪz kiː; aɪ praɪˈɔːrətaɪz.../", "健康飲食生活觀"));
        dl3_3.missions.add(new CourseModel.Mission(1, "分享自己目前的運動習慣（如慢跑/重訓/游泳/瑜珈）", "分享自己目前的運動習慣（如慢跑/重訓/游泳/瑜珈）", new String[]{"gym", "run", "running", "workout", "weights", "swim", "yoga", "walk"}));
        dl3_3.missions.add(new CourseModel.Mission(2, "分享一項健康飲食習慣或喜愛的健康餐 (Diet / Protein / Veggies)", "分享一項健康飲食習慣或喜愛的健康餐 (Diet / Protein / Veggies)", new String[]{"diet", "protein", "salad", "chicken", "water", "sugar", "healthy food"}));
        dl3_3.missions.add(new CourseModel.Mission(3, "設定下一個月的新目標並互相鼓勵 (Goal / Keep it up)", "設定下一個月的新目標並互相鼓勵 (Goal / Keep it up)", new String[]{"goal", "target", "keep it up", "routine", "stay consistent", "proud"}));
        du3.lessons.add(dl3_3);

        dailyTrack.units.add(du3);
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
