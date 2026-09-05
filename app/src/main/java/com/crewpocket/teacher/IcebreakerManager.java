package com.crewpocket.teacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IcebreakerManager {

    public static class Icebreaker {
        public String targetPhrase;
        public String nativeHint;
        public String emoji;

        public Icebreaker(String emoji, String targetPhrase, String nativeHint) {
            this.emoji = emoji;
            this.targetPhrase = targetPhrase;
            this.nativeHint = nativeHint;
        }
    }

    public interface GenerateCallback {
        void onSuccess(List<Icebreaker> generatedList);
        void onError(String errorMessage);
    }

    private static final Map<String, List<Icebreaker>> DYNAMIC_CACHE = new HashMap<String, List<Icebreaker>>();

    public static List<Icebreaker> getIcebreakersForScenario(String scenario, String tutorLang) {
        List<Icebreaker> list = new ArrayList<Icebreaker>();

        // If user already generated dynamic sentences for this scenario, include them first!
        String cacheKey = (scenario != null ? scenario.toLowerCase() : "default") + "_" + (tutorLang != null ? tutorLang.toLowerCase() : "en");
        if (DYNAMIC_CACHE.containsKey(cacheKey) && DYNAMIC_CACHE.get(cacheKey) != null && !DYNAMIC_CACHE.get(cacheKey).isEmpty()) {
            list.addAll(DYNAMIC_CACHE.get(cacheKey));
        }

        // Japanese specific presets
        if ("ja".equalsIgnoreCase(tutorLang)) {
            if ("travel".equalsIgnoreCase(scenario)) {
                list.add(new Icebreaker("🧳", "すみません、チェックインをお願いします。", "不好意思，我想辦理入住手續。"));
                list.add(new Icebreaker("✈️", "タクシー乗り場はどちらですか？", "請問計程車乘車處在哪裡？"));
                list.add(new Icebreaker("🍽️", "この近くでおすすめの郷土料理はありますか？", "這附近有推薦的在地特色料理嗎？"));
                list.add(new Icebreaker("🎟️", "京都までの往復切符を一枚ください。", "請給我一張到京都的來回車票。"));
                list.add(new Icebreaker("🛍️", "免税手続きはどちらでできますか？", "請問在哪裡可以辦理退稅？"));
                list.add(new Icebreaker("📍", "写真を撮っていただけますか？", "可以幫我們拍張照片嗎？"));
            } else if ("business".equalsIgnoreCase(scenario)) {
                list.add(new Icebreaker("💼", "お忙しいところ恐れ入ります。本日の議題を確認させてください。", "百忙之中打擾了，我們先確認一下今天的會議議程。"));
                list.add(new Icebreaker("📊", "第3四半期の進捗状況について共有いたします。", "向各位報告第三季度的進度狀況。"));
                list.add(new Icebreaker("🤝", "こちらの提案について、ご意見をいただけますでしょうか。", "關於這份提案，能否請您給予一些意見？"));
                list.add(new Icebreaker("📧", "後ほど議事録をメールにてお送りいたします。", "稍後我會透過電子郵件發送會議紀錄給您。"));
                list.add(new Icebreaker("🗓️", "来週の火曜日に再度ミーティングを設定しましょう。", "我們下週二再安排一次會議吧。"));
            } else {
                list.add(new Icebreaker("👋", "はじめまして、よろしくお願いいたします！", "初次見面，請多多指教！"));
                list.add(new Icebreaker("☕", "今日はお天気が良くて気持ちがいいですね。", "今天天氣真好，令人心情舒暢呢。"));
                list.add(new Icebreaker("🎌", "日本語の日常会話を練習したいです。", "我想練習日語日常口語對話。"));
                list.add(new Icebreaker("🌸", "おすすめの観光地やカフェはありますか？", "有推薦的觀光景點或咖啡廳嗎？"));
                list.add(new Icebreaker("✨", "もう一度ゆっくり言っていただけますか？", "可以請您再慢一點說一次嗎？"));
            }
            return list;
        }

        // Korean specific presets
        if ("ko".equalsIgnoreCase(tutorLang)) {
            if ("travel".equalsIgnoreCase(scenario)) {
                list.add(new Icebreaker("🧳", "체크인 부탁드립니다. 예약자 이름은 김철수입니다.", "麻煩辦理入住，預訂人姓名是金哲洙。"));
                list.add(new Icebreaker("🍽️", "여기 시그니처 메뉴가 무엇인가요?", "這裡的招牌特色菜是什麼？"));
                list.add(new Icebreaker("🛍️", "이거 다른 색상이나 사이즈 있나요?", "這個有其他顏色或尺寸嗎？"));
                list.add(new Icebreaker("🚕", "명동역으로 가주세요. 감사합니다.", "請帶我到明洞站，謝謝。"));
                list.add(new Icebreaker("💳", "카드 결제 가능한가요?", "可以刷卡結帳嗎？"));
            } else {
                list.add(new Icebreaker("👋", "안녕하세요! 오늘 하루 어떠셨어요?", "你好！今天過得如何？"));
                list.add(new Icebreaker("☕", "주말에 보통 무엇을 하면서 시간을 보내세요?", "週末通常都做些什麼來打發時間？"));
                list.add(new Icebreaker("🇰🇷", "한국어 회화를 더 자연스럽게 하고 싶어요.", "我想讓韓語口說變得更自然。"));
                list.add(new Icebreaker("✨", "조금만 더 천천히 말씀해 주실 수 있나요?", "可以請您說得稍微慢一點嗎？"));
            }
            return list;
        }

        // Default English / Universal Extensive Library (8-10 practical sentences per scenario)
        if ("travel".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🧳", "Hi! I have a reservation under the name Alex for two nights.", "你好，我有兩晚的住宿預訂，登記名字是 Alex。"));
            list.add(new Icebreaker("✈️", "Excuse me, where can I catch the airport express train into downtown?", "不好意思，請問在哪裡可以搭乘直達市區的機場快線？"));
            list.add(new Icebreaker("🍽️", "Could you recommend a cozy local restaurant that serves authentic regional dishes?", "你能推薦一家供應道地特色料理的溫馨在地餐廳嗎？"));
            list.add(new Icebreaker("🎟️", "I would like two round-trip tickets to Central Station, please.", "請給我兩張前往中央車站的來回車票。"));
            list.add(new Icebreaker("🏨", "Is it possible to request a late check-out tomorrow around 1 PM?", "請問明天可以安排下午 1 點左右延遲退房嗎？"));
            list.add(new Icebreaker("🛍️", "Where is the tax refund counter located in this terminal?", "請問這個航廈的退稅櫃台在哪裡？"));
            list.add(new Icebreaker("🗺️", "Could you show me the best walking route to the historic museum?", "您可以跟我說一下去歷史博物館最佳的步行路線嗎？"));
            list.add(new Icebreaker("☕", "Is breakfast included, and what time is it served?", "請問有附早餐嗎？供應時間是幾點到幾點？"));
        } else if ("business".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("💼", "Good morning team, let's quickly align on today's priority agenda items.", "大家早安，我們先快速對齊今天優先要討論的議程。"));
            list.add(new Icebreaker("📊", "Could you give us a brief status update on the Q3 product deliverables?", "可以請您簡要更新一下第三季度產品交付成果的進度嗎？"));
            list.add(new Icebreaker("🤝", "I propose we finalize the scope of work before sending the contract to the client.", "我建議在將合約發給客戶之前，我們先敲定工作範疇。"));
            list.add(new Icebreaker("💡", "From my perspective, automating this workflow will save at least 20 hours per sprint.", "依我看，將這個工作流程自動化每個衝刺能省下至少 20 小時。"));
            list.add(new Icebreaker("📈", "What are the key performance metrics and user feedback since the latest launch?", "自最新版本上線以來，關鍵成效指標與用戶反饋如何？"));
            list.add(new Icebreaker("🎯", "Let's schedule a 30-minute follow-up on Thursday to review the revised proposal.", "我們週四排個 30 分鐘的後續會議來審視修改後的提案。"));
            list.add(new Icebreaker("💰", "How does this implementation impact our quarterly infrastructure budget?", "這項實作會對我們季度的雲端架構預算產生什麼影響？"));
            list.add(new Icebreaker("🚀", "I will send out the detailed meeting summary and action items by end of day.", "我會在今天下班前寄出詳細的會議總結與待辦清單。"));
        } else if ("interview".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("👔", "Thank you for the opportunity to speak with you today about this role.", "非常感謝您今天給我機會來面試這個職位。"));
            list.add(new Icebreaker("🌟", "I have over 4 years of hands-on experience leading full-stack software development.", "我有四年以上帶領全端軟體開發的實戰經驗。"));
            list.add(new Icebreaker("🎯", "One of my proudest achievements was reducing API latency by 45% through caching.", "我最引以為傲的成就之一是透過快取機制將 API 延遲降低了 45%。"));
            list.add(new Icebreaker("🤝", "When cross-functional disagreements arise, I prioritize open data-driven communication.", "當跨部門產生分歧時，我優先秉持透明且以數據為導向的溝通方式。"));
            list.add(new Icebreaker("💡", "I'm particularly drawn to your mission of democratizing AI language education.", "我特別受貴公司普及 AI 語言教育的使命所吸引。"));
            list.add(new Icebreaker("🚀", "Could you share more about the day-to-day challenges this engineering team faces?", "您可以多分享一些這個工程團隊平時面臨的主要挑戰嗎？"));
            list.add(new Icebreaker("📈", "What does a successful first 90 days look like for someone in this position?", "對於這個職位的人來說，前 90 天成功的樣貌會是如何？"));
            list.add(new Icebreaker("🏢", "How would you describe the collaboration culture between product and engineering here?", "您會如何形容這裡產品團隊與工程團隊之間的協作文化？"));
        } else if ("exam".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("📝", "I'm ready for the speaking task. Please give me the topic card and instructions.", "我準備好進行口說任務了，請給我題目卡與指示。"));
            list.add(new Icebreaker("🗣️", "In my opinion, artificial intelligence will fundamentally reshape personalized learning.", "在我看來，人工智慧將會從根本上重塑個人化學習模式。"));
            list.add(new Icebreaker("⚖️", "While there are undeniable economic benefits, we must also weigh the environmental costs.", "雖然有不可否認的經濟效益，但我們也必須權衡環境成本。"));
            list.add(new Icebreaker("🌱", "For instance, sustainable urban planning can significantly alleviate traffic congestion.", "例如，可持續的都市規劃能顯著緩解交通壅塞問題。"));
            list.add(new Icebreaker("📈", "Could you evaluate my lexical variety, grammatical accuracy, and pronunciation flow?", "可以評估我的詞彙豐富度、文法準確性與發音流暢度嗎？"));
            list.add(new Icebreaker("🔍", "To look at this from another perspective, cultural diversity enriches global creativity.", "從另一個角度來看，文化多樣性豐富了全球的創意思維。"));
            list.add(new Icebreaker("💡", "Consequently, proactive government policies are essential to address this challenge.", "因此，積極主動的政府政策是解決這項挑戰不可或缺的。"));
            list.add(new Icebreaker("🎯", "Let's move on to the follow-up discussion on modern lifestyle shifts.", "我們接著進行關於現代生活型態轉變的後續討論吧。"));
        } else if ("shopping".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🛍️", "Excuse me, do you happen to have this linen shirt in a size medium?", "不好意思，請問這件亞麻襯衫有 M 號尺寸嗎？"));
            list.add(new Icebreaker("🏷️", "Is there any seasonal promotion or bundle discount going on today?", "今天有任何換季促銷或是組合折扣活動嗎？"));
            list.add(new Icebreaker("👗", "Could you tell me where the fitting rooms are located?", "可以告訴我更衣室在哪裡嗎？"));
            list.add(new Icebreaker("💳", "Do you accept international credit cards and mobile contactless payments?", "你們接受國際信用卡和手機感應式支付嗎？"));
            list.add(new Icebreaker("🧾", "Could I have a gift receipt and a separate shopping bag, please?", "可以給我一張禮品收據和一個額外的購物袋嗎？"));
            list.add(new Icebreaker("✈️", "What is the minimum spending threshold to qualify for airport tax refund?", "符合機場退稅資格的最低消費門檻是多少？"));
            list.add(new Icebreaker("🔄", "What is your return and exchange policy if the fit isn't quite right?", "如果尺寸不太合身，你們的退換貨政策是怎樣的？"));
            list.add(new Icebreaker("🎨", "Does this classic trench coat come in beige or navy blue as well?", "這款經典風衣有米色或是深藍色款嗎？"));
        } else if ("medical".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🩺", "Doctor, I've had a persistent dry cough, sore throat, and mild fever since yesterday.", "醫生，我從昨天開始一直持續乾咳、喉嚨痛而且有點低燒。"));
            list.add(new Icebreaker("💊", "How many times a day should I take this antibiotic, and should it be after meals?", "這個抗生素一天要吃幾次？需要飯後吃嗎？"));
            list.add(new Icebreaker("🩹", "I have a mild allergy to penicillin, so please prescribe an alternative.", "我對青黴素有輕微過敏，請幫我開替代藥物。"));
            list.add(new Icebreaker("🌡️", "Are there any common side effects like drowsiness or dizziness I should expect?", "有什麼常見的副作用（如嗜睡或頭暈）是我需要注意的嗎？"));
            list.add(new Icebreaker("🛌", "How many days of rest do you recommend before I can resume physical workouts?", "在恢復健身運動之前，您建議休息幾天？"));
            list.add(new Icebreaker("📋", "Could you write me a doctor's note for two days of sick leave for my workplace?", "可以幫我開一張兩天病假的醫生證明給公司嗎？"));
            list.add(new Icebreaker("🩸", "Do I need to fast before coming in for the routine blood test tomorrow morning?", "明天早上來做常規抽血檢查前需要空腹嗎？"));
            list.add(new Icebreaker("🏥", "If my symptoms worsen over the weekend, when should I visit the urgent care clinic?", "如果週末症狀加重，我什麼時候該去急診診所？"));
        } else if ("dining".equalsIgnoreCase(scenario) || "restaurant".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🍽️", "Good evening, we'd like a table for two by the window, please.", "晚安，我們想要一張靠窗的兩人桌，謝謝。"));
            list.add(new Icebreaker("🍷", "What are the chef's special recommendations for appetizers and wine pairing?", "主廚有推薦什麼招牌開胃菜與佐餐紅酒嗎？"));
            list.add(new Icebreaker("🥩", "I will have the ribeye steak medium-rare with roasted vegetables on the side.", "我要一份肋眼牛排五分熟，旁邊搭配烤蔬菜。"));
            list.add(new Icebreaker("🥗", "Could we please make the pasta vegetarian with gluten-free options?", "可以幫我們把義大利麵做成素食且無麩質的嗎？"));
            list.add(new Icebreaker("🍰", "Could we see the dessert menu and order two hot cappuccinos?", "可以看一下甜點菜單並點兩杯熱卡布奇諾嗎？"));
            list.add(new Icebreaker("💳", "Excuse me, could we have the check, and is it possible to split the bill?", "不好意思可以買單嗎？請問可以分開付帳嗎？"));
            list.add(new Icebreaker("🥡", "Could we get a box to take home the remaining slices of pizza?", "可以給我們一個盒子打包剩下的披薩嗎？"));
            list.add(new Icebreaker("🥂", "Everything was delicious! Please compliment the chef for this wonderful meal.", "每道菜都太美味了！請代我向主廚致謝。"));
        } else if ("housing".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🔑", "Hi, I'm calling to inquire about the furnished one-bedroom apartment listed on Main St.", "你好，我是想詢問 Main St 上刊登的那間附家具一房公寓。"));
            list.add(new Icebreaker("🚿", "Are high-speed internet, heating, and water included in the monthly rent?", "高速網路、暖氣和水費有包含在每月租金中嗎？"));
            list.add(new Icebreaker("📦", "When would be a convenient time to schedule an in-person or virtual walkthrough?", "什麼時候方便預約實地或線上視訊看房呢？"));
            list.add(new Icebreaker("🐾", "Is the building pet-friendly, and is there any additional security deposit required?", "這棟大樓允許養寵物嗎？需要額外繳交寵物押金嗎？"));
            list.add(new Icebreaker("🚗", "Does the rental unit include a reserved covered parking spot or storage unit?", "租屋單位有包含專屬遮雨停車位或儲藏室嗎？"));
            list.add(new Icebreaker("📝", "What is the standard lease duration, and what documents do I need for application?", "標準租期是多久？申請需要準備哪些文件？"));
            list.add(new Icebreaker("🛠️", "How does the property management handle emergency maintenance and repairs?", "物業管理平時是如何處理緊急維修與修繕的？"));
            list.add(new Icebreaker("🌳", "How safe is the surrounding neighborhood at night, and where is the nearest grocery store?", "周邊街區晚上的治安如何？最近的超市在哪裡？"));
        } else if ("dating".equalsIgnoreCase(scenario) || "social".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("☕", "Hi there! How has your week been treating you so far?", "嗨！這週過得還順心愉快嗎？"));
            list.add(new Icebreaker("🎬", "Have you watched any captivating movies, documentaries, or shows recently?", "你最近有看什麼吸引人的電影、紀錄片或影集嗎？"));
            list.add(new Icebreaker("🍕", "What is your absolute go-to comfort food after a long tiring day?", "經歷漫長疲憊的一天後，你最療癒的美食是什麼？"));
            list.add(new Icebreaker("✈️", "If you could catch a flight anywhere in the world tomorrow, where would you fly?", "如果明天能搭飛機去世界上的任何地方，你會飛去哪？"));
            list.add(new Icebreaker("🎵", "What kind of music or podcasts do you usually listen to during your daily commute?", "你平時通勤時最常聽什麼類型的音樂或 Podcast？"));
            list.add(new Icebreaker("🐕", "Are you more of an outdoor adventure person or do you enjoy cozy indoor weekends?", "你比較偏向戶外探險派，還是喜歡舒適待在室內的週末？"));
            list.add(new Icebreaker("📚", "What is a fascinating hobby or skill you've always wanted to pick up?", "有什麼有趣的愛好或技能是你一直想學的？"));
            list.add(new Icebreaker("✨", "That sounds incredible! Tell me more about how you got started with that.", "聽起來太棒了！多跟我說說你是怎麼開始接觸這個的。"));
        } else if ("tech".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("💻", "Let's review our microservices architecture and discuss API rate limiting strategies.", "我們來審視微服務架構並討論 API 速率限制策略。"));
            list.add(new Icebreaker("🤖", "How are you handling retrieval-augmented generation and prompt latency in production?", "你們在生產環境中是如何處理 RAG 檢索增強生成與 Prompt 延遲的？"));
            list.add(new Icebreaker("🚀", "What's your take on recent multi-agent frameworks and autonomous coding assistants?", "你對最近的多 Agent 智慧體框架與自主編程助手有什麼看法？"));
            list.add(new Icebreaker("🔍", "We noticed a memory leak in the background worker thread during peak load testing.", "我們在尖峰負載測試中發現後台工作執行緒有記憶體洩漏現象。"));
            list.add(new Icebreaker("🐳", "We automated our CI/CD pipeline with GitHub Actions and Kubernetes deployment.", "我們用 GitHub Actions 和 K8s 實現了 CI/CD 自動化部署流程。"));
            list.add(new Icebreaker("📊", "What database indexing optimizations helped you scale to ten million daily active users?", "哪些資料庫索引優化幫助你們擴展到千萬級日活躍用戶？"));
            list.add(new Icebreaker("🛡️", "How do you ensure end-to-end encryption and compliance across all client nodes?", "你們如何確保所有客戶端節點之間的端到端加密與合規性？"));
            list.add(new Icebreaker("⚡", "Let's refactor this module into clean modular components with full unit test coverage.", "我們把這個模組重構成乾淨的模組化元件並補齊單元測試覆蓋率吧。"));
        } else if ("guide".equalsIgnoreCase(scenario) || "onboarding".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("👋", "Hello tutor! Nice to meet you, I'm excited to start practicing speaking!", "哈囉導師！很高興見到你，我很期待開始練習口說！"));
            list.add(new Icebreaker("🎯", "My main goal is to improve my speaking confidence for daily conversations.", "我的主要目標是提升日常會話中的開口自信度。"));
            list.add(new Icebreaker("🗣️", "Could you speak a little slower and correct my grammar whenever I make mistakes?", "你可以說得慢一點，並在我犯錯時及時糾正我的文法嗎？"));
            list.add(new Icebreaker("💡", "How do I ask you to explain complex words in Chinese during our lessons?", "在課堂中我該如何請你用中文解釋較難的單字呢？"));
            list.add(new Icebreaker("✈️", "I'm preparing for an upcoming overseas trip and want to drill travel phrases.", "我正在準備即將到來的出國旅行，想練習旅遊會話。"));
            list.add(new Icebreaker("💼", "Can we practice English for job interviews and business presentations?", "我們可以練習求職面試與商務簡報的英語嗎？"));
            list.add(new Icebreaker("🔥", "How many minutes of oral practice per day do you recommend for best results?", "您建議每天練習幾分鐘口說能達到最佳進步效果？"));
            list.add(new Icebreaker("🚀", "I'm ready for my first quick speaking drill! What should we talk about first?", "我準備好進行第一次快速口說練習了！我們先聊聊什麼？"));
        } else {
            // General / Daily Life
            list.add(new Icebreaker("👋", "Hello! It's great to chat with you today. How is everything going?", "哈囉！今天很高興跟你聊天，一切都還順利嗎？"));
            list.add(new Icebreaker("☀️", "The weather is really pleasant today. Have you spent any time outdoors?", "今天天氣真的很舒服，你有去戶外走走嗎？"));
            list.add(new Icebreaker("☕", "I just made a fresh cup of coffee to kickstart my study session.", "我剛泡了一杯新鮮咖啡，準備開始我的學習時光。"));
            list.add(new Icebreaker("🎬", "I've been looking for some good podcast recommendations. Do you have any favorites?", "我最近在尋找好看的 Podcast 推薦，你有什麼私房名單嗎？"));
            list.add(new Icebreaker("🍕", "What did you have for lunch or dinner today? Any great food discoveries?", "你今天午餐或晚餐吃了什麼？有發現什麼美味料理嗎？"));
            list.add(new Icebreaker("🎯", "I'd like to practice discussing my daily routines and hobbies today.", "今天我想練習聊聊我的日常生活作息與休閒愛好。"));
            list.add(new Icebreaker("✨", "Could you suggest a fun topic or question for us to discuss together?", "你能提供一個有趣的題目或問題讓我們一起討論嗎？"));
            list.add(new Icebreaker("💡", "If I struggle with vocabulary, please guide me with natural phrases.", "如果我在詞彙上卡住，請用道地的說法引導我。"));
        }

        return list;
    }

    public static void generateAsync(final Context context, final String scenario, final String tutorLangCode, final String studentLangCode, final String currentContext, final GenerateCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(new Runnable() {
                @Override public void run() { callback.onError("Please configure Gemini API Key first"); }
            });
            return;
        }

        String tutorLangName = MainActivity.getLanguageLabel(tutorLangCode);
        String studentLangName = MainActivity.getLanguageLabel(studentLangCode);
        String scenarioLabel = scenario != null && !scenario.isEmpty() ? scenario : "Daily Conversation";

        final String prompt = "You are an expert language teacher crafting practical, authentic, level-appropriate spoken example sentences for a student.\n\n"
                + "Scenario Topic: " + scenarioLabel + "\n"
                + "Target Spoken Language: " + tutorLangName + " (" + tutorLangCode + ")\n"
                + "Student Native Language for Translations: " + studentLangName + " (" + studentLangCode + ")\n"
                + (currentContext != null && !currentContext.isEmpty() ? ("Current Context: " + currentContext + "\n") : "")
                + "\nREQUIREMENTS:\n"
                + "1. Generate 6 distinct, highly practical, authentic speaking starter / response sentences in " + tutorLangName + ".\n"
                + "2. Sentences MUST sound like what a real native speaker would say in this scenario.\n"
                + "3. Provide a clear, natural translation in " + studentLangName + " for each sentence.\n"
                + "4. Pick a fitting single emoji for each sentence (e.g. 🧳, ☕, 💼, 🍽️, ✈️, 💡, 🎯, 🛍️, 🗣️).\n"
                + "5. Output ONLY a valid JSON array of objects with keys: \"emoji\", \"targetPhrase\", \"nativeHint\".\n\n"
                + "Example JSON format:\n"
                + "[\n"
                + "  {\"emoji\":\"🧳\",\"targetPhrase\":\"Excuse me, where is the baggage claim area?\",\"nativeHint\":\"不好意思，請問行李提取區在哪裡？\"},\n"
                + "  {\"emoji\":\"☕\",\"targetPhrase\":\"Could I have an iced latte with oat milk, please?\",\"nativeHint\":\"請給我一杯冰拿鐵換燕麥奶，謝謝。\"}\n"
                + "]";

        GeminiApiClient.generateText(context, prompt, new GeminiApiClient.TextCallback() {
            @Override
            public void onSuccess(final String rawText) {
                final List<Icebreaker> resultList = new ArrayList<Icebreaker>();
                try {
                    String jsonStr = rawText.trim();
                    int start = jsonStr.indexOf("[");
                    int end = jsonStr.lastIndexOf("]");
                    if (start >= 0 && end > start) {
                        jsonStr = jsonStr.substring(start, end + 1);
                    }
                    JSONArray array = new JSONArray(jsonStr);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String emoji = obj.optString("emoji", "💬");
                        String target = obj.optString("targetPhrase", "");
                        String hint = obj.optString("nativeHint", "");
                        if (!target.isEmpty()) {
                            resultList.add(new Icebreaker(emoji, target, hint));
                        }
                    }
                } catch (Exception err) {
                    // Fallback parse lines if JSON formatting had issues
                    String[] lines = rawText.split("\n");
                    for (String l : lines) {
                        l = l.trim();
                        if (l.length() > 3 && !l.startsWith("[") && !l.startsWith("]") && !l.startsWith("{")) {
                            resultList.add(new Icebreaker("✨", l.replaceAll("^[0-9\\.\\-\\*\"]+", "").trim(), "AI 生成範例句"));
                        }
                    }
                }

                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        if (!resultList.isEmpty()) {
                            String cacheKey = (scenario != null ? scenario.toLowerCase() : "default") + "_" + (tutorLangCode != null ? tutorLangCode.toLowerCase() : "en");
                            DYNAMIC_CACHE.put(cacheKey, resultList);
                            callback.onSuccess(resultList);
                        } else {
                            callback.onError("Failed to parse AI generated sentences");
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        callback.onError(errorMessage);
                    }
                });
            }
        });
    }

    public static void clearCache() {
        DYNAMIC_CACHE.clear();
    }
}

