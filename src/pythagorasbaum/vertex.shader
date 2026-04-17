// OpenGL Shading Language

#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in mat4 instanceMatrix;
layout (location = 5) in vec3 aNormal; // Normalen-Daten
layout (location = 6) in float aDepth;

uniform mat4 projection;
uniform mat4 view;

out float vDepth;
out vec3 vNormal;
out vec3 vFragPos; // Position des Pixels in der Welt

void main() {
    // Berechnung der Position des Eckpunktes in der Welt
    vec4 worldPos = instanceMatrix * vec4(aPos, 1.0);
    vFragPos = worldPos.xyz;

    // Normale muss in Welt-Koordinaten rotiert werden
    // mat3, um nur die Rotation der Instanz zu übernehmen
    vNormal = mat3(instanceMatrix) * aNormal;
    vDepth = aDepth;

    gl_Position = projection * view * worldPos;
}
