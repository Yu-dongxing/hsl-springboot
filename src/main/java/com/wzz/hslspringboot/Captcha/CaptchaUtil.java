package com.wzz.hslspringboot.Captcha;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CaptchaUtil {
    public void IMAGE_CLICK(CaptchaData captchaData) {
        String data=captchaData.getExtra();
        String[] dataArray = data.split("\\|");
        JSONArray jsonArray = new JSONArray();
        int t = 0;
        for (int i = 0; i < dataArray.length; i++) {
            String[] XY = dataArray[i].split(",");
            JSONObject obj = new JSONObject();
            if (i>0){
                t += 120 + new Random().nextInt(1200);
            }
            Random random = new Random();
            int randomNum = random.nextInt(9);
            Random random2 = new Random();
            int randomNum2 = random2.nextInt(9);
            obj.put("x", Integer.parseInt(XY[0])/2+randomNum);
            obj.put("y",Integer.parseInt(XY[1])/2+randomNum2);
            obj.put("type", "click");
            obj.put("t", t);
            jsonArray.add(obj);
        }
        captchaData.setTrackList(jsonArray);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = now.minusSeconds(t/1000);
        String nowStr = now.toString();
        String lastStr = last.toString();
        nowStr = nowStr.substring(0, nowStr.length() - 6)+"Z";
        lastStr = lastStr.substring(0, lastStr.length() - 6)+"Z";
        captchaData.setStartTime(lastStr);
        captchaData.setStopTime(nowStr);
    }
    public void SLIDER(CaptchaData captchaData){
        int data = (int) captchaData.getResult();
        List<TrackPoint> track = generate(data, System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < track.size(); i++) {
            sb.append(track.get(i));
            if (i < track.size() - 1) sb.append(",");
        }
        sb.append("]");
        JSONArray jsonArray = JSONArray.parseArray(sb.toString());
        int t = jsonArray.getJSONObject(jsonArray.size() - 1).getIntValue("t");
//        // 1. 使用 Instant 类来处理时间戳，它本身就是UTC时间。
//        Instant stopTime = Instant.now();
//
//        // 2. 使用 minusMillis() 来精确地减去耗时，避免精度丢失。
//        Instant startTime = stopTime.minusMillis(t);
//
//        // 3. Instant.toString() 会自动生成标准 ISO-8601 格式 (例如 "2025-08-25T14:42:50.123Z")
//        //    这个格式是所有现代JSON解析库都能正确识别的。
//        captchaData.setStartTime(startTime.toString());
//        captchaData.setStopTime(stopTime.toString());
//
        captchaData.setTrackList(jsonArray);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = now.minusSeconds(t / 1000);

// 转成 OffsetDateTime（带时区信息）
        OffsetDateTime nowUtc = now.atOffset(ZoneOffset.UTC);
        OffsetDateTime lastUtc = last.atOffset(ZoneOffset.UTC);

// 使用 ISO 格式化
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        String nowStr = nowUtc.format(formatter);
        String lastStr = lastUtc.format(formatter);

        captchaData.setStartTime(lastStr);
        captchaData.setStopTime(nowStr);
    }


    /**
     * 生成“真人”风格的滑块轨迹：
     * - 初始 x=100±10，y=570±30
     * - 随机 1-3 段平稳 + 1-2 段落差
     * - 平稳：每移动 1-3px 记录一次，dt 短
     * - 落差：先长停顿（也记录 y），再大步前进，dt 稍长
     * - y：整体“逐步往上增加”（数值增大），抖动幅度极小并随时间逐步减小；
     *      落差阶段抖动缩小 3 倍；每步后高概率插入一个“微抖点”，使波动次数≈×2
     * - 输出包含 down/move/up 与严格递增的 t（毫秒）
     */
    public static class TrackPoint {
        public int x, y, t;
        public String type; // "down"/"move"/"up"
        public TrackPoint(int x, int y, String type, int t) { this.x=x; this.y=y; this.type=type; this.t=t; }
        @Override public String toString() {
            return String.format("{\"x\":%d,\"y\":%d,\"type\":\"%s\",\"t\":%d}", x, y, type, t);
        }
    }

    public static List<TrackPoint> generate(int distanceX, Long seed) {
        if (distanceX <= 0) throw new IllegalArgumentException("distanceX 必须为正数");
        Random rnd = (seed == null) ? new Random() : new Random(seed);

        int startX = 100 + randInt(rnd, -10, 10);
        int baseY  = 570 + randInt(rnd, -30, 30);
        int yMin = baseY - 30, yMax = baseY + 30;

        int smoothCnt = randInt(rnd, 1, 3);
        int dropCnt   = randInt(rnd, 1, 2);
        int totalSeg  = smoothCnt + dropCnt;

        int[] segLens = splitDistance(rnd, distanceX, totalSeg);

        List<String> segTypes = new ArrayList<>();
        for (int i = 0; i < smoothCnt; i++) segTypes.add("smooth");
        for (int i = 0; i < dropCnt; i++)   segTypes.add("drop");
        Collections.shuffle(segTypes, rnd);

        List<TrackPoint> path = new ArrayList<>();
        int curX = startX;
        int curY = clamp(baseY + randInt(rnd, -2, 2), yMin, yMax);
        int t = 0;

        // y 上漂 + 阻尼抖动控制
        final double[] driftAcc = new double[]{0.0}; // 满1 → y += 1
        double jitterScale = 1.0;
        final double jitterDecay = 0.7;
        final double jitterFloor = 0.09;

        // 平台（贴近 yMax）启动阈值：滑动过半开始
        final double plateauStart = 0.50;
        // 平台目标（如担心越界，可换成 yMax-1 或 yMax-2）
        final int plateauTarget = yMax;
        // 平台抖动极小（只允许偶尔-1）
        final double plateauUpProb = 0.15; // 更少随机上抖（基本不上抖）
        final int plateauJitterUpMax = 1;  // 不允许 + 抖
        final int plateauJitterDownMax = 1;// 仅 0 或 -1 的极小“回弹”

        path.add(new TrackPoint(curX, curY, "down", t));

        for (int i = 0; i < totalSeg; i++) {
            String kind = segTypes.get(i);
            int segLen = segLens[i];

            if ("smooth".equals(kind)) {
                int remain = segLen;
                while (remain > 0) {
                    int step = Math.min(remain, randInt(rnd, 1, 3));
                    remain -= step;

                    int dtStep = randInt(rnd, 12, 28);
                    t += dtStep;
                    curX += step;

                    // —— 计算是否进入平台期 —— //
                    double progress = (curX - startX) / (double) distanceX;
                    boolean usePlateau = progress >= plateauStart;

                    if (usePlateau) {
                        // 平台：快速贴近 yMax，并在 yMax 附近更平稳
                        curY = updateYUpwardWithPlateau(
                                rnd, curY, yMin, yMax, driftAcc,
                                /*driftRateToTarget*/ 0.40, // 更快靠拢目标
                                plateauTarget,
                                /*baseJitterScale*/ Math.max(jitterFloor, jitterScale / 3.0),
                                plateauUpProb,
                                plateauJitterUpMax,
                                plateauJitterDownMax
                        );
                    } else {
                        // 正常：更平稳的上漂 + 极小抖动
                        curY = updateYUpward(
                                rnd, curY, yMin, yMax, driftAcc,
                                /*driftRate*/ 0.18,
                                /*baseJitterScale*/ jitterScale,
                                /*upProb*/ 0.60,
                                /*jitterUpMax*/ 1,
                                /*jitterDownMax*/ 0
                        );
                    }

                    jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                    path.add(new TrackPoint(curX, curY, "move", t));

                    // 额外“微抖点”（不动x），让波动次数更多但幅度极小
                    if (!usePlateau && rnd.nextDouble() < 0.68) {
                        int dtMicro = randInt(rnd, 8, 16);
                        t += dtMicro;
                        curY = updateYUpward(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.10, Math.max(jitterFloor, jitterScale * 0.5),
                                0.60, 1, 0
                        );
                        jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                        path.add(new TrackPoint(curX, curY, "move", t));
                    } else if (usePlateau && rnd.nextDouble() < 0.30) {
                        // 平台期：更低概率插入极小微抖点，确保几乎贴边
                        int dtMicro = randInt(rnd, 8, 16);
                        t += dtMicro;
                        curY = updateYUpwardWithPlateau(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.18, plateauTarget,
                                Math.max(jitterFloor, jitterScale/4.0),
                                plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                        );
                        jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                        path.add(new TrackPoint(curX, curY, "move", t));
                    }
                }

                // 平稳段末尾微停顿（平台期依然贴边）
                if (rnd.nextDouble() < 0.22) {
                    int pauses = randInt(rnd, 1, 2);
                    for (int k = 0; k < pauses; k++) {
                        t += randInt(rnd, 20, 45);
                        double progress = (curX - startX) / (double) distanceX;
                        boolean usePlateau = progress >= plateauStart;
                        if (usePlateau) {
                            curY = updateYUpwardWithPlateau(
                                    rnd, curY, yMin, yMax, driftAcc,
                                    0.18, plateauTarget, Math.max(jitterFloor, jitterScale/4.0),
                                    plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                            );
                        } else {
                            curY = updateYUpward(rnd, curY, yMin, yMax, driftAcc,
                                    0.10, Math.max(jitterFloor, jitterScale*0.6), 0.60, 1, 0);
                        }
                        jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                        path.add(new TrackPoint(curX, curY, "move", t));
                    }
                }

            } else {
                // 落差段（不平稳）：y 抖动缩小 3 倍；平台期仍贴边
                double dropScale = Math.max(jitterFloor/3.0, jitterScale / 3.0);

                // 长停顿
                int idle = randInt(rnd, 260, 820);
                int idleSlices = randInt(rnd, 2, 5);
                int each = Math.max(40, idle / idleSlices);
                for (int s = 0; s < idleSlices; s++) {
                    t += randInt(rnd, each - 20, each + 40);
                    double progress = (curX - startX) / (double) distanceX;
                    boolean usePlateau = progress >= plateauStart;
                    if (usePlateau) {
                        curY = updateYUpwardWithPlateau(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.28, plateauTarget, dropScale/2.0,
                                plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                        );
                    } else {
                        curY = updateYUpward(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.20, dropScale, 0.58, 1, 0
                        );
                    }
                    jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                    dropScale   = Math.max(jitterFloor/3.0, dropScale * jitterDecay);
                    path.add(new TrackPoint(curX, curY, "move", t));
                }

                // 快速推进 x
                int remain = segLen;
                while (remain > 0) {
                    int step = Math.min(remain, randInt(rnd, 3, 8));
                    remain -= step;

                    int dtStep = randInt(rnd, 14, 32);
                    t += dtStep;
                    curX += step;

                    double progress = (curX - startX) / (double) distanceX;
                    boolean usePlateau = progress >= plateauStart;
                    if (usePlateau) {
                        curY = updateYUpwardWithPlateau(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.30, plateauTarget, dropScale/2.0,
                                plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                        );
                    } else {
                        curY = updateYUpward(
                                rnd, curY, yMin, yMax, driftAcc,
                                0.26, dropScale, 0.58, 1, 0
                        );
                    }
                    jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                    dropScale   = Math.max(jitterFloor/3.0, dropScale * jitterDecay);
                    path.add(new TrackPoint(curX, curY, "move", t));

                    // 小概率额外微抖点
                    if (rnd.nextDouble() < (usePlateau ? 0.25 : 0.35)) {
                        t += randInt(rnd, 8, 16);
                        if (usePlateau) {
                            curY = updateYUpwardWithPlateau(
                                    rnd, curY, yMin, yMax, driftAcc,
                                    0.16, plateauTarget, dropScale/3.0,
                                    plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                            );
                        } else {
                            curY = updateYUpward(
                                    rnd, curY, yMin, yMax, driftAcc,
                                    0.12, Math.max(jitterFloor/3.0, dropScale*0.5), 0.58, 1, 0
                            );
                        }
                        jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                        dropScale   = Math.max(jitterFloor/3.0, dropScale * jitterDecay);
                        path.add(new TrackPoint(curX, curY, "move", t));
                    }
                }
            }
        }

        // 末尾轻校正
        int targetX = startX + distanceX;
        if (curX != targetX) {
            int delta = targetX - curX;
            int step = Integer.signum(delta) * Math.min(Math.abs(delta), 3);
            if (step != 0) {
                t += randInt(rnd, 15, 40);
                curX += step;
                // 末尾也按平台处理（通常已过半）
                curY = updateYUpwardWithPlateau(
                        rnd, curY, yMin, yMax, driftAcc,
                        0.20, plateauTarget, Math.max(jitterFloor, jitterScale/3.0),
                        plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
                );
                jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
                path.add(new TrackPoint(curX, curY, "move", t));
            }
        }

        // 收尾
        if (rnd.nextDouble() < 0.30) {
            t += randInt(rnd, 20, 60);
            curY = updateYUpwardWithPlateau(
                    rnd, curY, yMin, yMax, driftAcc,
                    0.16, plateauTarget, Math.max(jitterFloor, jitterScale/3.0),
                    plateauUpProb, plateauJitterUpMax, plateauJitterDownMax
            );
            jitterScale = Math.max(jitterFloor, jitterScale * jitterDecay);
            path.add(new TrackPoint(curX, curY, "move", t));
        }
        t += randInt(rnd, 10, 40);
        path.add(new TrackPoint(curX, curY, "up", t));

        return path;
    }

    /* —— 普通上漂 + 极小抖动 —— */
    private static int updateYUpward(
            Random rnd, int curY, int yMin, int yMax,
            double[] driftAcc,
            double driftRate,
            double baseJitterScale,
            double upProb,
            int jitterUpMax,
            int jitterDownMax
    ) {
        driftAcc[0] += driftRate;
        while (driftAcc[0] >= 1.0) { curY += 1; driftAcc[0] -= 1.0; }

        int upAmp   = Math.max(0, (int)Math.round(jitterUpMax   * baseJitterScale));
        int downAmp = Math.max(0, (int)Math.round(jitterDownMax * baseJitterScale));

        if (upAmp == 0 && downAmp == 0) {
            // no jitter
        } else if (rnd.nextDouble() < upProb) {
            if (upAmp > 0) curY += randInt(rnd, 1, upAmp);
        } else {
            if (downAmp > 0) curY -= randInt(rnd, 0, downAmp);
        }

        return clamp(curY, yMin, yMax);
    }

    /* —— 平台期：贴近 yMax 的上漂 + 极微抖（不允许越过目标） —— */
    private static int updateYUpwardWithPlateau(
            Random rnd, int curY, int yMin, int yMax,
            double[] driftAcc,
            double driftRateToTarget,
            int plateauTarget,
            double baseJitterScale,
            double upProb,
            int jitterUpMax,
            int jitterDownMax
    ) {
        // 目标收敛：如果还没到目标，用较快 drift 拉上去；到达后固定在目标
        if (curY < plateauTarget) {
            driftAcc[0] += driftRateToTarget;
            while (driftAcc[0] >= 1.0 && curY < plateauTarget) {
                curY += 1;
                driftAcc[0] -= 1.0;
            }
            if (curY > plateauTarget) curY = plateauTarget;
        } else {
            // 避免越界
            curY = Math.min(curY, plateauTarget);
        }

        // 极小抖动（平台）：只允许 0 或 -1 的下抖，基本不上抖
        int upAmp   = Math.max(0, (int)Math.round(jitterUpMax   * baseJitterScale));   // 通常是 0
        int downAmp = Math.max(0, (int)Math.round(jitterDownMax * baseJitterScale));   // 0或1

        if (downAmp > 0) {
            if (rnd.nextDouble() >= upProb) {
                curY -= randInt(rnd, 0, downAmp); // 稍微离开边界一点点
                if (curY < yMin) curY = yMin;
            }
        }
        // 若上抖允许（通常为0），也不能超过 plateauTarget
        if (upAmp > 0 && rnd.nextDouble() < upProb) {
            curY = Math.min(plateauTarget, curY + randInt(rnd, 1, upAmp));
        }

        return clamp(curY, yMin, yMax);
    }

    /* —— 工具方法 —— */
    private static int randInt(Random rnd, int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + rnd.nextInt(b - a + 1);
    }
    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
    private static int[] splitDistance(Random rnd, int distance, int n) {
        if (n <= 0) throw new IllegalArgumentException("n > 0");
        if (distance < n) {
            int[] r = new int[n];
            for (int i = 0; i < distance; i++) r[i] = 1;
            return r;
        }
        int[] parts = new int[n];
        Arrays.fill(parts, 1);
        int remain = distance - n;
        while (remain > 0) { parts[rnd.nextInt(n)]++; remain--; }
        for (int i = 0; i < n; i++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            int t = parts[a]; parts[a] = parts[b]; parts[b] = t;
        }
        return parts;
    }


}
