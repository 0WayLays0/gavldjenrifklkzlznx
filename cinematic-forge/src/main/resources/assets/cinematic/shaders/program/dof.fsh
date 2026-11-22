#version 150

// Separable lens blur: the circle of confusion grows with the distance to the focus plane.
// The focus plane is the depth at the centre of the screen (autofocus) unless FocusDepth >= 0.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform vec2 Direction;
uniform float DofStrength;
uniform float FocusDepth;
uniform float FarPlane;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

float linearDepth(vec2 uv) {
    float z = texture(DepthSampler, uv).r * 2.0 - 1.0;
    float near = 0.05;
    return (2.0 * near * FarPlane) / (FarPlane + near - z * (FarPlane - near));
}

float coc(float d, float focus) {
    // Blur in pixels: none near the focus plane, up to 9 px far from it
    float diff = abs(d - focus) / max(focus * 1.5, 2.0);
    return clamp(diff, 0.0, 1.0) * 9.0 * DofStrength;
}

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    if (DofStrength <= 0.001) { fragColor = base; return; }
    float focus = FocusDepth >= 0.0 ? FocusDepth
        : min(min(linearDepth(vec2(0.5)), linearDepth(vec2(0.47, 0.5))), linearDepth(vec2(0.53, 0.5)));
    float radius = coc(linearDepth(texCoord), focus);
    if (radius < 0.5) { fragColor = base; return; }
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int i = -6; i <= 6; i++) {
        float t = float(i) / 6.0;
        vec2 uv = texCoord + Direction * oneTexel * t * radius;
        // Sharp foreground must not bleed into the blurred background
        float w = (1.0 - abs(t) * 0.5) * clamp(coc(linearDepth(uv), focus) / max(radius, 0.001) + 0.15, 0.0, 1.0);
        sum += texture(DiffuseSampler, uv).rgb * w;
        total += w;
    }
    fragColor = vec4(sum / max(total, 0.0001), 1.0);
}
