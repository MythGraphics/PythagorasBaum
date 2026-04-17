// OpenGL Shading Language
// Fragment Shader == Pixel Shader -> Fragment == Pixel

#version 330 core

struct Material {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};
  
in float vDepth;
in vec3 vNormal;
in vec3 vFragPos;

uniform Material material;
uniform vec3 viewPos; // Die aktuelle Position der Kamera (camX, camY, camZ aus Java)
uniform float maxDepth;

out vec4 FragColor;

void main() {
    // Farben definieren
    vec3 lightColor = vec3(1.0, 1.0, 1.0); // weiß
    vec3 rootColor  = vec3(0.4, 0.2, 0.1); // braun
    vec3 leafColor  = vec3(0.1, 0.8, 0.2); // grün
    
    // Grundfarbe basierend auf der Tiefe
    vec3 baseColor;
    if (vDepth < 0.0) {
        // Bodenfarbe (Dunkelgrau)
        baseColor = vec3(0.2, 0.2, 0.2);
    } else {
        float factor = clamp(vDepth / maxDepth, 0.0, 1.0);
        baseColor    = mix(leafColor, rootColor, factor);
    }

    // Beleuchtungs-Vektoren
    vec3 normal     = normalize(vNormal);
    vec3 lightDir   = normalize( vec3( 1.0, 1.0, 0.2 )); // Lichtquelle rechts (x) oben (y) etwas vor der Betrachtungsebene (z)
    vec3 viewDir    = normalize(viewPos - vFragPos);
    vec3 reflectDir = reflect(-lightDir, normal);

    // Ambient: Grundhelligkeit im Schatten
    vec3 ambient = material.ambient * baseColor;

    // Diffuses Licht: Lambert-Reflexion
    // Das Skalarprodukt dot() berechnet den Kosinus des Winkels zwischen Normale und Licht
    float diff   = max( dot( normal, lightDir ), 0.0 );
    vec3 diffuse = (diff * material.diffuse) * baseColor;

    // Specular: Glanzlicht
    float spec    = pow( max( dot( viewDir, reflectDir ), 0.0 ), material.shininess );
    vec3 specular = (spec * material.specular); // unabhängig von baseColor (weiß)

    // Finales Ergebnis: Farbe * Helligkeit
    vec3 result = ambient + diffuse + specular;
    FragColor = vec4(result, 1.0);
}
