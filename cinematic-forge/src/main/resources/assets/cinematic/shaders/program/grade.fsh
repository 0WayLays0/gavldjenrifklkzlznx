#version 150

// Film look: chromatic aberration, soft bloom, teal & orange grade, filmic curve,
// vignette, animated grain, letterbox bars and fades to black.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Clock;
uniform float Bars;
uniform float Fade;
uniform float Grade;
uniform float Grain;
uniform float Vignette;
uniform float Aberration;
uniform float Bloom;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec2 uv = texCoord;
    vec2 c = uv - 0.5;

    // Chromatic aberration, stronger toward the edges like a real lens
    vec2 shift = c * dot(c, c) * 0.012 * Aberration;
    vec3 col;
    col.r = texture(DiffuseSampler, uv + shift).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, uv - shift).b;

    // Cheap bloom: bright neighbours glow
    if (Bloom > 0.001) {
        vec3 glow = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.785398;
            vec2 o = vec2(cos(a), sin(a)) * oneTexel * 6.0;
            vec3 s = texture(DiffuseSampler, uv + o).rgb + texture(DiffuseSampler, uv + o * 2.5).rgb;
            glow += max(s - 1.3, 0.0);
        }
        col += glow / 16.0 * Bloom * 2.0;
    }

    // Grade: teal shadows, warm highlights, a touch more contrast, slightly less saturation
    vec3 graded = col;
    float luma = dot(graded, vec3(0.2126, 0.7152, 0.0722));
    graded = mix(vec3(luma), graded, 1.12);
    graded += mix(vec3(-0.02, 0.025, 0.05), vec3(0.06, 0.02, -0.04), smoothstep(0.15, 0.85, luma));
    graded = aces(graded * 1.25);
    graded = pow(graded, vec3(1.05));
    col = mix(col, graded, Grade);

    // Vignette
    float v = smoothstep(0.85, 0.2, length(c * vec2(OutSize.x / OutSize.y, 1.0)) * 0.9);
    col *= mix(1.0, v, Vignette);

    // Grain, changing every frame (24 fps feel)
    float frame = floor(Clock * 24.0);
    float n = hash(uv * OutSize + frame * 17.0) - 0.5;
    col += n * Grain * (1.0 - luma * 0.5);

    col *= 1.0 - Fade;

    // Letterbox to 2.39:1, sliding in with Bars (0..1)
    float screenAspect = OutSize.x / OutSize.y;
    float barH = max(0.0, (1.0 - screenAspect / 2.39) * 0.5) * Bars;
    if (uv.y < barH || uv.y > 1.0 - barH) col = vec3(0.0);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
