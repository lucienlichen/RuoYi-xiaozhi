package com.clda.chat.core;

import java.util.*;

/**
 * 情绪分析器：根据文本内容判断主导情绪，返回对应的 emoji 名称
 */
public class EmotionAnalyzer {

    // 表情对应的 emoji 映射表
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();

    // 情绪关键词库
    private static final Map<String, List<String>> EMOTION_KEYWORDS = new HashMap<>();

    // 情绪优先级（用于得分相同的情况下决策）
    private static final List<String> PRIORITY_EMOTIONS = List.of(
            "laughing", "crying", "angry", "surprised", "shocked",
            "loving", "happy", "funny", "cool",
            "sad", "embarrassed", "confused",
            "thinking", "winking", "relaxed",
            "delicious", "kissy", "confident", "sleepy", "silly"
    );

    static {
        // 初始化 emoji 映射
        Map<String, String> emojiInit = Map.ofEntries(
                Map.entry("neutral", "\uD83D\uDE36"),     // 😶
                Map.entry("happy", "\uD83D\uDE42"),       // 🙂
                Map.entry("laughing", "\uD83D\uDE06"),    // 😆
                Map.entry("funny", "\uD83D\uDE02"),       // 😂
                Map.entry("sad", "\uD83D\uDE14"),         // 😔
                Map.entry("angry", "\uD83D\uDE20"),       // 😠
                Map.entry("crying", "\uD83D\uDE2D"),      // 😭
                Map.entry("loving", "\uD83D\uDE0D"),      // 😍
                Map.entry("embarrassed", "\uD83D\uDE33"), // 😳
                Map.entry("surprised", "\uD83D\uDE32"),   // 😲
                Map.entry("shocked", "\uD83D\uDE31"),     // 😱
                Map.entry("thinking", "\uD83E\uDD14"),    // 🤔
                Map.entry("winking", "\uD83D\uDE09"),     // 😉
                Map.entry("cool", "\uD83D\uDE0E"),        // 😎
                Map.entry("relaxed", "\uD83D\uDE0C"),     // 😌
                Map.entry("delicious", "\uD83E\uDD24"),   // 🤤
                Map.entry("kissy", "\uD83D\uDE18"),       // 😘
                Map.entry("confident", "\uD83D\uDE0F"),   // 😏
                Map.entry("sleepy", "\uD83D\uDE34"),      // 😴
                Map.entry("silly", "\uD83D\uDE1C"),       // 😜
                Map.entry("confused", "\uD83D\uDE44")     // 🙄
        );
        EMOJI_MAP.putAll(emojiInit);
    }

    static {
        // 初始化情绪关键词
        addEmotionKeywords("happy", "开心", "高兴", "快乐", "愉快", "幸福", "满意", "棒", "好", "不错", "完美", "棒极了", "太好了", "好呀", "好的", "happy", "joy", "great", "good", "nice", "awesome", "fantastic", "wonderful");
        addEmotionKeywords("laughing", "哈哈", "哈哈哈", "呵呵", "嘿嘿", "嘻嘻", "笑死", "太好笑了", "笑死我了", "lol", "lmao", "haha", "hahaha", "hehe", "rofl", "funny", "laugh");
        addEmotionKeywords("funny", "搞笑", "逗", "笑点", "有趣", "笑喷", "喜剧", "funny", "humor", "comedy");
        addEmotionKeywords("sad", "伤心", "难过", "悲哀", "悲伤", "忧郁", "郁闷", "沮丧", "失望", "想哭", "难受", "不开心", "唉", "呜呜", "sad", "upset", "unhappy", "depressed", "sorrow", "gloomy");
        addEmotionKeywords("angry", "生气", "愤怒", "气死", "讨厌", "烦人", "可恶", "烦死了", "恼火", "暴躁", "火大", "气炸了", "angry", "mad", "annoyed", "furious", "pissed", "hate");
        addEmotionKeywords("crying", "哭", "眼泪", "流泪", "悲伤", "难过", "哭泣", "tears", "crying", "sob", "weep");
        addEmotionKeywords("loving", "喜欢", "爱", "亲", "爱你", "我爱你", "么么", "亲亲", "表白", "喜欢你", "love", "like", "miss", "adore", "affection");
        addEmotionKeywords("embarrassed", "尴尬", "不好意思", "脸红", "囧", "羞", "embarrassed", "awkward", "shy", "blush");
        addEmotionKeywords("surprised", "惊讶", "惊奇", "震惊", "意外", "吃惊", "哇", "呀", "wow", "surprised", "amazed", "shocked", "unexpected");
        addEmotionKeywords("shocked", "震惊", "崩溃", "吓死", "吓人", "惊悚", "shocked", "scared", "horrified", "terrified");
        addEmotionKeywords("thinking", "思考", "考虑", "想一下", "琢磨", "沉思", "冥想", "想", "思考中", "在想", "think", "thinking", "consider", "ponder", "meditate");
        addEmotionKeywords("winking", "眨眼", "调皮", "wink", "playful", "teasing");
        addEmotionKeywords("cool", "酷", "厉害", "牛", "太强", "优秀", "你真棒", "你真好", "帅", "cool", "awesome", "great", "amazing");
        addEmotionKeywords("relaxed", "放松", "休闲", "悠闲", "惬意", "relaxed", "calm", "chill", "easygoing");
        addEmotionKeywords("delicious", "好吃", "美味", "香", "吃货", "馋", "delicious", "tasty", "yummy", "mouthwatering");
        addEmotionKeywords("kissy", "亲", "吻", "亲亲", "么么哒", "kiss", "muah", "smooch");
        addEmotionKeywords("confident", "自信", "有信心", "我可以", "我行", "相信自己", "confident", "believe", "sure", "capable");
        addEmotionKeywords("sleepy", "困", "瞌睡", "想睡", "晚安", "睡觉", "累了", "sleepy", "tired", "sleep", "bedtime");
        addEmotionKeywords("silly", "傻", "笨", "呆", "二", "silly", "goofy", "dumb", "foolish");
        addEmotionKeywords("confused", "困惑", "疑惑", "搞不懂", "搞混", "混乱", "糊涂", "confused", "puzzled", "unclear", "lost");
    }

    /**
     * 根据情绪关键词追加到关键词库
     */
    private static void addEmotionKeywords(String emotion, String... keywords) {
        EMOTION_KEYWORDS.put(emotion, List.of(keywords));
    }

    /**
     * 将情绪名转换为 emoji 字符
     */
    public static String mappingEmoji(String emotion) {
        return EMOJI_MAP.getOrDefault(emotion, EMOJI_MAP.get("happy"));
    }

    /**
     * 根据文本内容进行情感分析
     */
    public static String analyzeEmotion(String input) {
        if (input == null || input.trim().isEmpty()) return "neutral";

        String original = input.trim();
        String normalized = original.toLowerCase();

        // 如果文本中包含 emoji，直接返回对应情绪
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            if (original.contains(entry.getValue())) return entry.getKey();
        }

        // 标点符号特征
        boolean hasExclamation = original.contains("!") || original.contains("！");
        boolean hasQuestion = original.contains("?") || original.contains("？");
        boolean hasEllipsis = original.contains("...") || original.contains("…");

        // 固定句型优先判断
        if (containsAny(normalized, "you are", "you're", "你真棒", "你好厉害", "so smart", "so kind")) return "loving";
        if (containsAny(normalized, "i am", "我太棒了", "我真厉害", "so good", "so happy")) return "cool";
        if (containsAny(normalized, "睡觉", "晚安", "sleep", "good night", "go to bed")) return "sleepy";
        if (hasQuestion && !hasExclamation) return "thinking";
        if (hasExclamation && !hasQuestion) {
            if (containsEmotions(normalized, "happy", "laughing", "cool")) return "laughing";
            if (containsEmotions(normalized, "angry", "sad", "crying")) return "angry";
            return "surprised";
        }
        if (hasEllipsis) return "thinking";

        // 情绪打分机制
        Map<String, Double> emotionScores = new HashMap<>();
        for (String key : EMOJI_MAP.keySet()) emotionScores.put(key, 0.0);

        EMOTION_KEYWORDS.forEach((emotion, keywords) -> {
            for (String keyword : keywords) {
                if (normalized.contains(keyword)) {
                    double base = 1.0;
                    if (normalized.length() > 20) {
                        int freq = normalized.split(keyword, -1).length - 1;
                        base += freq * 0.5;
                    }
                    emotionScores.put(emotion, emotionScores.get(emotion) + base);
                }
            }
        });

        double max = Collections.max(emotionScores.values());
        if (max == 0) return "happy";

        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Double> entry : emotionScores.entrySet()) {
            if (entry.getValue().equals(max)) candidates.add(entry.getKey());
        }

        for (String e : PRIORITY_EMOTIONS) {
            if (candidates.contains(e)) return e;
        }
        return candidates.getFirst();
    }

    /**
     * 是否包含任意关键词
     */
    private static boolean containsAny(String text, String... keywords) {
        for (String word : keywords) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    /**
     * 是否包含任意情绪下的关键词
     */
    private static boolean containsEmotions(String text, String... emotions) {
        for (String e : emotions) {
            for (String keyword : EMOTION_KEYWORDS.getOrDefault(e, Collections.emptyList())) {
                if (text.contains(keyword)) return true;
            }
        }
        return false;
    }
}
