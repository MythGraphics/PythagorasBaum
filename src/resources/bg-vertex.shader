// OpenGL Shading Language

# version 330 core

layout (location = 0) in vec2 aPos;
out vec2 vPos;

void main() {
    vPos = aPos;
    gl_Position = vec4(aPos, 1.0, 1.0); // z=1.0 setzt es ganz nach hinten
}
