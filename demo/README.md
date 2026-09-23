# Cómo probar este plugin

Este plugin le muestra al programador cuánto "pesa" cada instrucción
de un archivo `Dockerfile` (un archivo de configuración que se usa
para empaquetar aplicaciones).

## Qué hacer

1. En el panel de la izquierda, buscá y abrí el archivo llamado
   **`Dockerfile`** (dentro de la carpeta `demo`).
2. Al lado de algunas líneas vas a ver un texto extra en gris, a la
   derecha — eso lo agrega el plugin, no está en el archivo original.

## Qué deberías ver

- En las líneas que dicen `COPY ...`: un número (el tamaño real del
  archivo/carpeta que se está copiando).
- En la línea que dice `RUN apt-get update...`: un aviso de que ese
  comando es "conocido por ser pesado" — sin ningún número, porque
  el plugin no puede saber el tamaño exacto sin ejecutar el comando
  de verdad (y prefiere avisar honestamente en vez de inventar un
  número).
- En la línea `ADD https://...` — **nuevo en 0.2.0** — un aviso de "no
  se puede calcular" porque descarga de la red, NO el aviso de
  "archivo no encontrado" (serían cosas distintas).
- En la línea siguiente (`RUN apt-get install -y --no-install-recommends
  curl && rm -rf ...`) — **nuevo en 0.2.0** — NO debería aparecer
  ningún aviso: esa línea solo instala la herramienta `curl`, nunca la
  ejecuta para descargar algo.
- En la línea que dice `COPY --from=builder`: un aviso de "no se
  puede calcular" — tampoco un número inventado.

## Si algo no se ve así

Sacá la captura igual, y avisame qué línea no coincide con lo de
arriba (por ejemplo "la línea de COPY no muestra ningún número").
