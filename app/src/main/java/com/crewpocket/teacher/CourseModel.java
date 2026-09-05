package com.crewpocket.teacher;

import java.util.ArrayList;
import java.util.List;

public class CourseModel {

    public static class Track {
        public String id;
        public String titleEn;
        public String titleZh;
        public String icon;
        public String descriptionEn;
        public String descriptionZh;
        public List<Unit> units = new ArrayList<>();

        public Track(String id, String icon, String titleEn, String titleZh, String descriptionEn, String descriptionZh) {
            this.id = id;
            this.icon = icon;
            this.titleEn = titleEn;
            this.titleZh = titleZh;
            this.descriptionEn = descriptionEn;
            this.descriptionZh = descriptionZh;
        }

        public String getTitle(boolean en) { return en ? titleEn : titleZh; }
        public String getDescription(boolean en) { return en ? descriptionEn : descriptionZh; }
    }

    public static class Unit {
        public String id;
        public String trackId;
        public String titleEn;
        public String titleZh;
        public String descriptionEn;
        public String descriptionZh;
        public List<Lesson> lessons = new ArrayList<>();

        public Unit(String id, String trackId, String titleEn, String titleZh, String descriptionEn, String descriptionZh) {
            this.id = id;
            this.trackId = trackId;
            this.titleEn = titleEn;
            this.titleZh = titleZh;
            this.descriptionEn = descriptionEn;
            this.descriptionZh = descriptionZh;
        }

        public String getTitle(boolean en) { return en ? titleEn : titleZh; }
        public String getDescription(boolean en) { return en ? descriptionEn : descriptionZh; }
    }

    public static class WarmupPhrase {
        public String en;
        public String zh;
        public String ipa;
        public String note;

        public WarmupPhrase(String en, String zh, String ipa, String note) {
            this.en = en;
            this.zh = zh;
            this.ipa = ipa;
            this.note = note;
        }
    }

    public static class Mission {
        public int id;
        public String descEn;
        public String descZh;
        public String[] targetKeywords;
        public boolean achieved = false;

        public Mission(int id, String descEn, String descZh, String[] targetKeywords) {
            this.id = id;
            this.descEn = descEn;
            this.descZh = descZh;
            this.targetKeywords = targetKeywords;
        }

        public String getDesc(boolean en) { return en ? descEn : descZh; }
    }

    public static class Lesson {
        public String id;
        public String trackId;
        public String unitId;
        public String titleEn;
        public String titleZh;
        public String descriptionEn;
        public String descriptionZh;
        public String scenario; // Maps to persona / scenario
        public String promptInstruction; // Extra instruction for AI persona
        public int estimatedMins = 5;
        public List<WarmupPhrase> warmupPhrases = new ArrayList<>();
        public List<Mission> missions = new ArrayList<>();

        public Lesson(String id, String trackId, String unitId, String titleEn, String titleZh,
                      String descriptionEn, String descriptionZh, String scenario, String promptInstruction) {
            this.id = id;
            this.trackId = trackId;
            this.unitId = unitId;
            this.titleEn = titleEn;
            this.titleZh = titleZh;
            this.descriptionEn = descriptionEn;
            this.descriptionZh = descriptionZh;
            this.scenario = scenario;
            this.promptInstruction = promptInstruction;
        }

        public String getTitle(boolean en) { return en ? titleEn : titleZh; }
        public String getDescription(boolean en) { return en ? descriptionEn : descriptionZh; }
    }

    public static class LessonProgress {
        public String lessonId;
        public int stars = 0; // 0, 1, 2, 3
        public int bestScore = 0;
        public boolean completed = false;
        public long lastCompletedTime = 0;

        public LessonProgress() {}

        public LessonProgress(String lessonId, int stars, int bestScore, boolean completed, long lastCompletedTime) {
            this.lessonId = lessonId;
            this.stars = stars;
            this.bestScore = bestScore;
            this.completed = completed;
            this.lastCompletedTime = lastCompletedTime;
        }
    }
}
