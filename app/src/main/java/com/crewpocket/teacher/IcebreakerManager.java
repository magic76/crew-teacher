package com.crewpocket.teacher;

import java.util.ArrayList;
import java.util.List;

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

    public static List<Icebreaker> getIcebreakersForScenario(String scenario, String tutorLang) {
        List<Icebreaker> list = new ArrayList<Icebreaker>();

        if ("travel".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🧳", "Hi! I have a hotel reservation for two nights.", "你好，我有兩晚的旅館預訂"));
            list.add(new Icebreaker("✈️", "Excuse me, where can I find the airport taxi stand?", "不好意思，請問計程車乘車處在哪裡？"));
            list.add(new Icebreaker("🍽️", "Could you recommend some authentic local specialties?", "你能推薦一些道地的在地特色美食嗎？"));
        } else if ("business".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("💼", "Good morning, let's go over today's agenda.", "早安，我們先過一下今天會議的議程"));
            list.add(new Icebreaker("📊", "Could you give me a brief update on the Q3 milestones?", "可以請你簡要更新一下第三季度的里程碑進度嗎？"));
            list.add(new Icebreaker("🤝", "I propose we align on the deliverables before Friday.", "我建議我們在週五前對齊交付成果"));
        } else if ("interview".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("👔", "Thank you for the opportunity to interview today.", "非常感謝您今天給我面試的機會"));
            list.add(new Icebreaker("🌟", "I have 3+ years experience leading cross-functional teams.", "我有三年以上帶領跨職能團隊的經驗"));
            list.add(new Icebreaker("🎯", "Could you share more about the team culture here?", "您可以多分享一些關於這個團隊的文化嗎？"));
        } else if ("exam".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("📝", "I'm ready for Part 2. Please give me the topic card.", "我準備好進行第二部分了，請給我題目卡"));
            list.add(new Icebreaker("🗣️", "In my opinion, technology has transformed modern education.", "在我看來，科技深刻轉變了現代教育模式"));
            list.add(new Icebreaker("📈", "Could you evaluate my fluency and lexical resource?", "可以評估我的流暢度與詞彙多樣性嗎？"));
        } else if ("shopping".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🛍️", "Excuse me, do you have this shirt in medium?", "不好意思，這件衣服有 M 號尺寸嗎？"));
            list.add(new Icebreaker("💳", "Is there any discount or tax refund available?", "目前有任何折扣或是退稅服務嗎？"));
            list.add(new Icebreaker("🏷️", "Can I try this on in the fitting room?", "我可以拿到更衣室試穿一下嗎？"));
        } else if ("medical".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🩺", "Doctor, I've had a sore throat and fever since yesterday.", "醫生，我從昨天開始喉嚨痛而且有點發燒"));
            list.add(new Icebreaker("💊", "How many times a day should I take this medication?", "這個藥一天需要服用幾次？"));
            list.add(new Icebreaker("🩹", "Are there any side effects I should be aware of?", "有沒有什麼我需要注意的副作用？"));
        } else if ("housing".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("🔑", "Hi, I'm calling about the apartment for rent on Main St.", "你好，我是想詢問 Main St 上出租的公寓"));
            list.add(new Icebreaker("🚿", "Are utilities and internet included in the monthly rent?", "水電瓦斯與網路費有包含在每月租金中嗎？"));
            list.add(new Icebreaker("📦", "When would be a good time to schedule a room tour?", "什麼時候方便預約實地看房呢？"));
        } else if ("dating".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("☕", "Hi there! How has your week been treating you?", "嗨！這週過得還順心嗎？"));
            list.add(new Icebreaker("🎬", "Have you watched any great movies or shows recently?", "你最近有看什麼好看的電影或影集嗎？"));
            list.add(new Icebreaker("🍕", "What's your favorite comfort food on a rainy day?", "下雨天你最喜歡吃什麼療癒美食？"));
        } else if ("tech".equalsIgnoreCase(scenario)) {
            list.add(new Icebreaker("💻", "Let's discuss our system architecture and scalability.", "我們來討論系統架構與高併發擴展性"));
            list.add(new Icebreaker("🤖", "How are you integrating large language models into production?", "你們在生產環境中是如何整合大語言模型的？"));
            list.add(new Icebreaker("🚀", "What's your take on recent AI agent developments?", "你對最近 AI Agent 智慧體發展有什麼看法？"));
        } else {
            list.add(new Icebreaker("👋", "Hello! How is your day going so far?", "哈囉！你今天過得如何？"));
            list.add(new Icebreaker("☀️", "The weather is really nice today, isn't it?", "今天的天氣真的很不錯，對吧？"));
            list.add(new Icebreaker("☕", "What are your favorite plans for this coming weekend?", "你這個週末有什麼好玩期待的計畫嗎？"));
        }

        return list;
    }
}
