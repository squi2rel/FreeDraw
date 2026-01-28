package com.github.squi2rel.freedraw.render;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class BrushMath {
    /**
     * Catmull-Rom 插值
     * @param p0, p1, p2, p3 控制点
     * @param t [0, 1]
     * @param dest 输出
     */
    public static void catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, double t, Vector3d dest) {
        double t2 = t * t;
        double t3 = t2 * t;
        double f0 = -0.5 * t3 + t2 - 0.5 * t;
        double f1 =  1.5 * t3 - 2.5 * t2 + 1.0;
        double f2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
        double f3 =  0.5 * t3 - 0.5 * t2;
        dest.x = p0.x * f0 + p1.x * f1 + p2.x * f2 + p3.x * f3;
        dest.y = p0.y * f0 + p1.y * f1 + p2.y * f2 + p3.y * f3;
        dest.z = p0.z * f0 + p1.z * f1 + p2.z * f2 + p3.z * f3;
    }

    /**
     * 计算下一个法线方向（RMF - 旋转最小化标架）
     * 防止管线在空中自旋
     */
    public static void computeNextFrame(Vector3f lastTangent, Vector3f lastNormal, Vector3f currentTangent, Vector3f outNormal) {
        if (lastTangent == null || lastNormal == null) {
            // 初始帧：建立一个任意垂直于切线的法线
            Vector3f helper = Math.abs(currentTangent.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            currentTangent.cross(helper, outNormal).normalize();
        } else {
            // 计算从旧切线到新切线的最小旋转
            Quaternionf q = new Quaternionf().rotateTo(lastTangent, currentTangent);
            // 应用旋转到旧法线
            q.transform(lastNormal, outNormal).normalize();
        }
    }
}
