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

uniform Material material_base;
uniform Material material_top;
uniform vec3 viewPos; // Die aktuelle Position der Kamera (camX, camY, camZ aus Java)
uniform float maxDepth;

out vec4 FragColor;

void main() {
    // Mischfaktor berechnen
    float factor = clamp(vDepth / maxDepth, 0.0, 1.0);
    // Ein einfacher Abdunkelungs-Faktor basierend auf der Tiefe
    float shadowFactor = mix(1.0, 0.5, vDepth / maxDepth);
    float gamma = 1.4; // Gamma-Korrektur-Faktor
    float alpha = 1.0;
    
    // Farben definieren
    vec3 lightColor  = vec3(1.0, 1.0, 1.0); // weiß
    vec3 groundColor = vec3(0.4, 0.6, 0.9); // himmelblau
    vec3 rootColor   = vec3(0.4, 0.2, 0.1); // braun
    vec3 leafColor   = vec3(0.1, 0.8, 0.2); // grün
    
    // Materialeigenschaften definieren
    vec3 ambientColor;
    vec3 diffuseColor;
    vec3 specularColor;
    float shininess;
    
    // Grundfarben basierend auf der Tiefe
    vec3 baseColor;
    if (vDepth < 0.0) {
        alpha         = 0.5; // 50% Opazität
        baseColor     = groundColor;
        ambientColor  = material_base.ambient;
        diffuseColor  = material_base.diffuse;
        specularColor = material_base.specular;
        shininess     = material_base.shininess;
    } else {
        // Materialeigenschaften & -farbe interpolieren
        baseColor     = mix(leafColor, rootColor, factor);
        ambientColor  = mix(material_top.ambient,   material_base.ambient,   factor);
        diffuseColor  = mix(material_top.diffuse,   material_base.diffuse,   factor);
        specularColor = mix(material_top.specular,  material_base.specular,  factor);
        shininess     = mix(material_top.shininess, material_base.shininess, factor);
    }
    
    // Beleuchtungs-Vektoren
    vec3 normal     = normalize(vNormal);
    vec3 lightDir   = normalize( vec3( 1.0, 1.0, 0.2 )); // Lichtquelle rechts (x) oben (y) etwas vor der Betrachtungsebene (z)
    vec3 viewDir    = normalize(viewPos - vFragPos);
    vec3 reflectDir = reflect(-lightDir, normal);
    
    // Ambient: Grundhelligkeit im Schatten
    vec3 ambient = ambientColor * baseColor;
    
    // Diffuses Licht: Lambert-Reflexion
    // Das Skalarprodukt dot() berechnet den Kosinus des Winkels zwischen Normale und Licht
    float diff   = max( dot( normal, lightDir ), 0.0 );
    vec3 diffuse = (diff * diffuseColor) * baseColor * shadowFactor;
    
    // Specular: Glanzlicht
    float spec    = pow( max( dot( viewDir, reflectDir ), 0.0 ), shininess );
    vec3 specular = (spec * specularColor) * lightColor;
    
    // Finales Ergebnis: Farbe * Helligkeit
    vec3 result = ambient + diffuse + specular;
    result = pow( result, vec3( 1.0 / gamma )); // Gamma-Korrektur
    FragColor = vec4( clamp( result, 0.0, 1.0 ), alpha ); // optional: Begrenzung auf den sichtbaren Bereich
//  FragColor = vec4(result, alpha);

}
