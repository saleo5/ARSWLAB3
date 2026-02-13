# Laboratorio 3 ARSW

## Autor: Samuel Leonardo Albarracin Vergara

**Escuela Colombiana de Ingeniería -- Arquitecturas de Software**\
**Java 21 --- Concurrencia, sincronización y control de hilos**

------------------------------------------------------------------------

# Introducción

Este laboratorio se divide en tres partes principales:

-   **Parte I:** Productor/Consumidor --- comparación entre busy-wait y
    monitores (wait()/notifyAll()).
-   **Parte II:** Búsqueda distribuida con condición de parada
    anticipada.
-   **Parte III:** Simulación Highlander (Immortals & Synchronization)
    con UI Swing, análisis de regiones críticas y prevención de
    deadlocks.

El objetivo general fue entender cómo manejar correctamente la
concurrencia en Java, evitando condiciones de carrera, espera activa
innecesaria y bloqueos mutuos.

------------------------------------------------------------------------

# Parte I --- Productor / Consumidor

## ¿De qué trata esta parte?

En esta parte se trabajó con un programa clásico de Productor/Consumidor
probando dos enfoques distintos de sincronización:

-   **Busy-wait (espera activa)**
-   **Monitores de Java (synchronized, wait(), notifyAll())**

La idea fue comparar el comportamiento de ambos, especialmente en el
consumo de CPU, usando VisualVM.

------------------------------------------------------------------------

## Evidencia en VisualVM

### Escenario 1 --- Productor lento / Consumidor rápido

**Modo busy-wait:**

![Busy Wait Escenario 1](images/prueba1.png)

**Modo monitores:**

![Monitores Escenario 1](images/prueba2.png)

------------------------------------------------------------------------

### Escenario 2 --- Productor rápido / Consumidor lento (cola pequeña)

**Modo busy-wait:**

![Busy Wait Escenario 2](images/prueba3.png)

**Modo monitores:**

![Monitores Escenario 2](images/prueba4.png)

------------------------------------------------------------------------

## Análisis

El problema principal está en BusySpinQueue.\
Los métodos take() y put() usan ciclos while(true) que revisan
constantemente si hay espacio o elementos disponibles.

Aunque se usa Thread.onSpinWait(), el hilo nunca se bloquea realmente.
Simplemente sigue ejecutándose en CPU sin hacer trabajo útil, lo cual se
evidencia en las capturas anteriores.

En cambio, BoundedBuffer usa synchronized, wait() y notifyAll(),
lo que permite que los hilos se suspendan realmente cuando no pueden
continuar.

Esto evita consumo innecesario de CPU y respeta correctamente el límite
de la cola.

------------------------------------------------------------------------

# Parte II --- Búsqueda distribuida y condición de parada

En esta parte se modificó el buscador de listas negras para que no
recorriera todos los servidores si ya se habían encontrado las
ocurrencias necesarias para determinar si un host es malicioso.

------------------------------------------------------------------------

## Implementación del contador compartido

Se agregó un AtomicInteger compartido entre todos los hilos
(BlackListThread).

![Modificación en BlackListThread](images/bclthread2.png)

Cada hilo revisa si el contador ya alcanzó el límite
(BLACK_LIST_ALARM_COUNT).\
Si ya se alcanzó, termina su ejecución.\
Cuando encuentra una coincidencia, incrementa el contador con
incrementAndGet().

------------------------------------------------------------------------

## Integración en HostBlackListsValidator

En HostBlackListsValidator se creó un único AtomicInteger y se pasó
a todos los hilos para que compartieran el mismo contador.

![Modificación en HostBlackListsValidator](images/checkhost2.png)

------------------------------------------------------------------------

## Resultado

Con esta modificación:

-   La búsqueda se detiene apenas se alcanza el número necesario de
    ocurrencias.
-   No hay condiciones de carrera.
-   No fue necesario usar sincronización explícita.
-   Se optimiza el tiempo de ejecución.

------------------------------------------------------------------------

# Parte III --- Immortals & Synchronization (Highlander Simulator)

Simulación de N inmortales donde cada uno corre en su propio hilo y
ataca a otro al azar.

------------------------------------------------------------------------

## Funcionamiento general

Cada inmortal:

-   Resta damage al oponente.
-   Suma damage/2 a su propia vida.
-   Corre en un ciclo infinito hasta que muere o se detiene la
    simulación.

La pelea se ejecuta dentro de bloques synchronized.

------------------------------------------------------------------------

## Evidencias Parte III

*(Aquí puedes insertar tus capturas de la UI, pruebas con Pause & Check,
jstack, etc.)*

Ejemplo:

![UI en ejecución](images/prueba2seg)
![UI en ejecución resume](images/prueba8resume)
![Consulta naive congelado](images/consultanaivecongelado.png)
![jps](images/jps.png)
![Thread dump con jstack](images/jstack.png)
![Prueba con mas de 50](images/masde50.png)
![Prueba con mas de 100](images/masde100.png)
------------------------------------------------------------------------

## Pausa correcta

Se implementó PauseController usando:

-   ReentrantLock
-   Condition
-   AtomicInteger waitingCount

Cuando se llama pause():

-   Se activa una bandera.
-   Cada hilo llama a awaitIfPaused() y queda bloqueado.
-   Se espera hasta que todos los hilos estén pausados antes de leer la
    salud.

------------------------------------------------------------------------

## Prevención de deadlock

Se utilizó **orden total por nombre**.

Antes de hacer synchronized, se comparan los nombres y siempre se
adquieren los locks en el mismo orden.

Esto elimina la posibilidad de espera circular.

------------------------------------------------------------------------

# Conclusiones generales del laboratorio

Este laboratorio permitió entender de forma práctica:

-   Por qué la espera activa no es eficiente.
-   Cómo usar correctamente wait()/notifyAll().
-   Cómo evitar condiciones de carrera con AtomicInteger.
-   Cómo prevenir deadlocks mediante orden total.
-   Cómo diseñar pausas cooperativas correctamente.
-   Cómo trabajar con colecciones concurrentes.
-   Cómo diagnosticar problemas reales de concurrencia con herramientas
    como jstack.

Se logró una implementación funcional, robusta y consistente incluso con
un número alto de hilos.
