# Conclusiones

**Integrantes:** Luis David Mendoza Manzano

---

## Conclusiones del proyecto

Presente aquí las conclusiones del proyecto. Como mínimo debe responder:

1. **Programación funcional:** ¿Qué ventajas y dificultades encontraron al implementar
   la solución usando recursión y funciones de alto orden en lugar de ciclos iterativos?

    **R:** Lo mejor de usar este estilo fue que el código terminó pareciéndose mucho a las fórmulas matemáticas del enunciado. En lugar de andar contando con variables y bucles complicados, usamos funciones como filter o sum que hacen el trabajo "solas". Al principio costó un poco cambiar el chip para pensar en transformar listas en vez de dar pasos uno por uno, pero una vez que entendimos cómo construir las soluciones paso a paso (especialmente en la función que genera todas las asignaciones posibles), todo fluyó mucho mejor.

2. **Corrección:** ¿Cómo argumentaron formalmente que sus implementaciones son correctas?
   ¿Qué técnicas de inducción estructural o de invariantes aplicaron?

    **R:** Para estar seguros de que no había errores, usamos lógica matemática básica: si funciona para un caso pequeño y sabemos cómo pasar al siguiente, entonces funciona para todos. Además, probamos con los ejemplos exactos del taller (como esos costos de 1031 y 37) y el programa nos dio los mismos resultados. Eso nos dio la tranquilidad de que la lógica era correcta.

3. **Paralelismo:** ¿En qué escenarios resultó beneficioso paralelizar? ¿Cuándo la
   sobrecarga del sistema superó la ganancia esperada?

    **R:** Resultó beneficioso en casos con más de 4 cursos, antes de eso la sobrecarga de organizar los hilos de ejecución se lleva lo que se ahorra paralelizando. Al repetir varias veces el Benchmark se observa que hay un límite de aceleración de aproximadamente el 47%

4. **Aprendizajes:** ¿Qué conceptos del curso les resultaron más útiles para resolver
   el problema? ¿Qué cambiarían en su diseño si volvieran a empezar?
    
    **R:** En definitiva  fue la paralelización de tareas principalmente; Usar la abstracción para tomar esas fórmulas matemáticas y llevarlas al código; Saber en donde usar una cosa o la otra (Secuencial | Paralelización)


