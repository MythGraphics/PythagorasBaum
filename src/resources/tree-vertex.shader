// OpenGL Shading Language

#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in mat4 instanceMatrix;
layout (location = 5) in vec3 aNormal;
layout (location = 6) in float aDepth;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 uGroundMatrix;
uniform mat3 uGroundNormal;

out float vDepth;
out vec3 vNormal;
out vec3 vFragPos; // Position des Pixels in der Welt

void main() {
    mat4 model;
    mat3 normal;
    vDepth = aDepth;
    // wenn aDepth negativ, Boden zeichnen
    if (aDepth < 0.0) {
        model  = uGroundMatrix;
        normal = uGroundNormal;
    } else {
        model  = instanceMatrix;
        normal = mat3(model);
    }

    // Berechnung der Position des Eckpunktes in der Welt
    vec4 worldPos = model * vec4(aPos, 1.0);
    vFragPos = worldPos.xyz;

    // Normale muss in Welt-Koordinaten rotiert werden
    vNormal = normal * aNormal;

    gl_Position = projection * view * worldPos;
}
