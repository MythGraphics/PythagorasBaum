// OpenGL Shading Language

# version 330 core

out vec4 FragColor;
in vec2 vPos;

void main() {
    vec3 skyTopColor    = vec3(0.4, 0.6, 0.9); // himmelblau
    vec3 skyBottomColor = vec3(0.9, 0.9, 0.9); // dunstiges Weiß am Horizont

    // Mix basierend auf der vertikalen Position
    float factor = vPos.y * 0.5 + 0.5; // normalisiert auf 0.0 bis 1.0
    vec3 finalColor = mix(skyBottomColor, skyTopColor, factor);

    FragColor = vec4(finalColor, 1.0);
}
