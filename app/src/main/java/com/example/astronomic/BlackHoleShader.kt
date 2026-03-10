package com.example.astronomic

import android.opengl.GLES20

object BlackHoleShader {
    var program = 0

    fun init() {
        val vertexShader = """
            attribute vec4 aPosition;
            varying vec2 vUV;
            void main() {
                gl_Position = aPosition;
                vUV = aPosition.xy * 0.5 + 0.5;
            }
        """

        val fragmentShader = """
            precision highp float;
            varying vec2 vUV;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform vec2 uMouse;
            uniform float uBlackHoleX;
            uniform float uBlackHoleY;
            uniform float uBlackHoleZ;
            uniform sampler2D uBackgroundTexture;

            #define AA 1
            #define _Speed 3.0
            #define _Steps 12.0
            #define _Size 0.3

            float hash(float x) { return fract(sin(x) * 152754.742); }
            float hash(vec2 x) { return hash(x.x + hash(x.y)); }

            float value(vec2 p, float f) {
                float bl = hash(floor(p * f + vec2(0.0, 0.0)));
                float br = hash(floor(p * f + vec2(1.0, 0.0)));
                float tl = hash(floor(p * f + vec2(0.0, 1.0)));
                float tr = hash(floor(p * f + vec2(1.0, 1.0)));

                vec2 fr = fract(p * f);
                fr = (3.0 - 2.0 * fr) * fr * fr;
                float b = mix(bl, br, fr.x);
                float t = mix(tl, tr, fr.x);
                return mix(b, t, fr.y);
            }

            vec4 getBackground(vec3 ray) {
                // scale = 1.0 - картинка на всю сцену
                float scale = 1.0;
                vec2 uv = ray.xy * scale * 0.5 + 0.5;
                
                uv = clamp(uv, 0.0, 1.0);
                
                return texture2D(uBackgroundTexture, uv);
            }

            vec4 raymarchDisk(vec3 ray, vec3 zeroPos) {
                vec3 position = zeroPos;
                float lengthPos = length(position.xz);
                float dist = min(1.0, lengthPos * (1.0 / _Size) * 0.5) * _Size * 0.4 * (1.0 / _Steps) / (abs(ray.y));

                position += dist * _Steps * ray * 0.5;

                vec2 deltaPos;
                deltaPos.x = -zeroPos.z * 0.01 + zeroPos.x;
                deltaPos.y = zeroPos.x * 0.01 + zeroPos.z;
                deltaPos = normalize(deltaPos - zeroPos.xz);

                float parallel = dot(ray.xz, deltaPos);
                parallel /= sqrt(lengthPos);
                parallel *= 0.5;
                float redShift = parallel + 0.3;
                redShift *= redShift;
                redShift = clamp(redShift, 0.0, 1.0);

                float disMix = clamp((lengthPos - _Size * 2.0) * (1.0 / _Size) * 0.24, 0.0, 1.0);
                vec3 insideCol = mix(vec3(1.0, 0.8, 0.0), vec3(0.5, 0.13, 0.02) * 0.2, disMix);
                insideCol *= mix(vec3(0.4, 0.2, 0.1), vec3(1.6, 2.4, 4.0), redShift);
                insideCol *= 1.25;
                redShift += 0.12;
                redShift *= redShift;

                vec4 o = vec4(0.0);

                for (float i = 0.0; i < _Steps; i++) {
                    position -= dist * ray;

                    float intensity = clamp(1.0 - abs((i - 0.8) * (1.0 / _Steps) * 2.0), 0.0, 1.0);
                    float lengthPos = length(position.xz);
                    float distMult = 1.0;

                    distMult *= clamp((lengthPos - _Size * 0.75) * (1.0 / _Size) * 1.5, 0.0, 1.0);
                    distMult *= clamp((_Size * 10.0 - lengthPos) * (1.0 / _Size) * 0.20, 0.0, 1.0);
                    distMult *= distMult;

                    float u = lengthPos + uTime * _Size * 0.3 + intensity * _Size * 0.2;

                    vec2 xy;
                    float rot = mod(uTime * _Speed, 8192.0);
                    xy.x = -position.z * sin(rot) + position.x * cos(rot);
                    xy.y = position.x * sin(rot) + position.z * cos(rot);

                    float x = abs(xy.x / (xy.y));
                    float angle = 0.02 * atan(x);

                    const float f = 70.0;
                    float noise = value(vec2(angle, u * (1.0 / _Size) * 0.05), f);
                    noise = noise * 0.66 + 0.33 * value(vec2(angle, u * (1.0 / _Size) * 0.05), f * 2.0);

                    float extraWidth = noise * 1.0 * (1.0 - clamp(i * (1.0 / _Steps) * 2.0 - 1.0, 0.0, 1.0));

                    float alpha = clamp(noise * (intensity + extraWidth) * ((1.0 / _Size) * 10.0 + 0.01) * dist * distMult, 0.0, 1.0);

                    vec3 col = 2.0 * mix(vec3(0.3, 0.2, 0.15) * insideCol, insideCol, min(1.0, intensity * 2.0));
                    o = clamp(vec4(col * alpha + o.rgb * (1.0 - alpha), o.a * (1.0 - alpha) + alpha), 0.0, 1.0);

                    lengthPos *= (1.0 / _Size);
                    o.rgb += redShift * (intensity * 1.0 + 0.5) * (1.0 / _Steps) * 100.0 * distMult / (lengthPos * lengthPos);
                }

                o.rgb = clamp(o.rgb - 0.005, 0.0, 1.0);
                return o;
            }

            void main() {
                vec2 fragCoord = vUV * uResolution;
                vec4 colOut = vec4(0.0);
                
                for (int j = 0; j < AA; j++) {
                    for (int i = 0; i < AA; i++) {
                        vec3 ray = normalize(vec3((fragCoord - uResolution * 0.5 + vec2(float(i), float(j)) / float(AA)) / uResolution.x, 1.0));
                        vec3 pos = vec3(0.0, 0.05, -20.0);

                        vec2 angle = vec2(uTime * 0.1, 0.2);
                        angle.y = (2.0 * uMouse.y / uResolution.y) * 3.14 + 0.1 + 3.14;
                        float dist = length(pos);

                        float rotSpeed = min(0.3 / dist, 3.14);
                        angle.xy -= rotSpeed * vec2(1.0, 0.5);

                        pos.x += uBlackHoleX * 5.0;
                        pos.y += uBlackHoleY * 3.0;
                        pos.z += uBlackHoleZ * 2.0 - 15.0;

                        vec4 col = vec4(0.0);
                        vec4 glow = vec4(0.0);
                        vec4 outCol = vec4(100.0);

                        for (int disks = 0; disks < 20; disks++) {
                            for (int h = 0; h < 6; h++) {
                                float dotpos = dot(pos, pos);
                                float invDist = inversesqrt(dotpos);
                                float centDist = dotpos * invDist;
                                float stepDist = 0.92 * abs(pos.y / (ray.y));
                                float farLimit = centDist * 0.5;
                                float closeLimit = centDist * 0.1 + 0.05 * centDist * centDist * (1.0 / _Size);
                                stepDist = min(stepDist, min(farLimit, closeLimit));

                                float invDistSqr = invDist * invDist;
                                float bendForce = stepDist * invDistSqr * _Size * 0.625;
                                ray = normalize(ray - (bendForce * invDist) * pos);
                                pos += stepDist * ray;

                                glow += vec4(1.2, 1.1, 1.0, 1.0) * (0.01 * stepDist * invDistSqr * invDistSqr * clamp(centDist * 2.0 - 1.2, 0.0, 1.0));
                            }

                            float dist2 = length(pos);

                            if (dist2 < _Size * 0.1) {
                                outCol = vec4(col.rgb * col.a + glow.rgb * (1.0 - col.a), 1.0);
                                break;
                            } else if (dist2 > _Size * 1000.0) {
                                vec4 bgColor = getBackground(ray);
                                outCol = vec4(col.rgb * col.a + bgColor.rgb * (1.0 - col.a) + glow.rgb * (1.0 - col.a), 1.0);
                                break;
                            } else if (abs(pos.y) <= _Size * 0.002) {
                                vec4 diskCol = raymarchDisk(ray, pos);
                                pos.y = 0.0;
                                pos += abs(_Size * 0.001 / ray.y) * ray;
                                col = vec4(diskCol.rgb * (1.0 - col.a) + col.rgb, col.a + diskCol.a * (1.0 - col.a));
                            }
                        }

                        if (outCol.r == 100.0)
                            outCol = vec4(col.rgb + glow.rgb * (col.a + glow.a), 1.0);

                        col = outCol;
                        col.rgb = pow(col.rgb, vec3(0.6));
                        colOut += col / float(AA * AA);
                    }
                }

                gl_FragColor = colOut;
            }
        """

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, load(GLES20.GL_VERTEX_SHADER, vertexShader))
        GLES20.glAttachShader(program, load(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        GLES20.glLinkProgram(program)
    }

    private fun load(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}