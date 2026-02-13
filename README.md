
# ARSW — (Java 21): **Immortals & Synchronization** — con UI Swing

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de concurrencia: condiciones de carrera, sincronización, suspensión cooperativa y *deadlocks*, con interfaz **Swing** tipo *Highlander Simulator*.


---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

### Interfaz gráfica (Swing) — *Highlander Simulator*

**Opción A (desde Main, modo ui)**
bash
mvn -q -DskipTests exec:java -Dmode=ui -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10


**Opción B (clase de la UI directamente)**
bash
mvn -q -DskipTests exec:java -Dexec.mainClass=edu.eci.arsw.highlandersim.ControlFrame -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10


**Parámetros**  
- -Dcount=N → número de inmortales (por defecto 8)  
- -Dfight=ordered|naive → estrategia de pelea (ordered evita *deadlocks*, naive los puede provocar)  
- -Dhealth, -Ddamage → salud inicial y daño por golpe

### Demos teóricas (sin UI)
bash
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=1  # 1 = Deadlock ingenuo
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=2  # 2 = Orden total (sin deadlock)
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=3  # 3 = tryLock + timeout (progreso)


---

## Controles en la UI

- **Start**: inicia una simulación con los parámetros elegidos.
- **Pause & Check**: pausa **todos** los hilos y muestra salud por inmortal y **suma total** (invariante).
- **Resume**: reanuda la simulación.
- **Stop**: detiene ordenadamente.

**Invariante**: con N jugadores y salud inicial H, la **suma total** de salud debe permanecer constante (salvo durante un update en curso). Usa **Pause & Check** para validarlo.

---

## Arquitectura (carpetas)


edu.eci.arsw
├─ app/                 # Bootstrap (Main): modes ui|immortals|demos
├─ highlandersim/       # UI Swing: ControlFrame (Start, Pause & Check, Resume, Stop)
├─ immortals/           # Dominio: Immortal, ImmortalManager, ScoreBoard
├─ concurrency/         # PauseController (Lock/Condition; paused(), awaitIfPaused())
├─ demos/               # DeadlockDemo, OrderedTransferDemo, TryLockTransferDemo
└─ core/                # BankAccount, TransferService (para demos teóricas)


---

# Actividades del laboratorio

## Parte I — (Antes de terminar la clase) wait/notify: Productor/Consumidor
1. Ejecuta el programa de productor/consumidor y monitorea CPU con **jVisualVM**. ¿Por qué el consumo alto? ¿Qué clase lo causa?  
2. Ajusta la implementación para **usar CPU eficientemente** cuando el **productor es lento** y el **consumidor es rápido**. Valida de nuevo con VisualVM.  
3. Ahora **productor rápido** y **consumidor lento** con **límite de stock** (cola acotada): garantiza que el límite se respete **sin espera activa** y valida CPU con un stock pequeño.

> Nota: la Parte I se realiza en el repositorio dedicado https://github.com/DECSIS-ECI/Lab_busy_wait_vs_wait_notify — clona ese repo y realiza los ejercicios allí; contiene el código de productor/consumidor, variantes con busy-wait y las soluciones usando wait()/notify(), además de instrucciones para ejecutar y validar con jVisualVM.


> Usa monitores de Java: **synchronized + wait() + notify/notifyAll()**, evitando *busy-wait*.

---

## Parte II — (Antes de terminar la clase) Búsqueda distribuida y condición de parada
Reescribe el **buscador de listas negras** para que la búsqueda **se detenga tan pronto** el conjunto de hilos detecte el número de ocurrencias que definen si el host es confiable o no (BLACK_LIST_ALARM_COUNT). Debe:
- **Finalizar anticipadamente** (no recorrer servidores restantes) y **retornar** el resultado.  
- Garantizar **ausencia de condiciones de carrera** sobre el contador compartido.

> Puedes usar AtomicInteger o sincronización mínima sobre la región crítica del contador.

---

## Parte III — (Avance) Sincronización y *Deadlocks* con *Highlander Simulator*

1. Revisa la simulación: N inmortales; cada uno **ataca** a otro. El que ataca **resta M** al contrincante y **suma M/2** a su propia vida.

   > Cada inmortal corre en su propio hilo. En un ciclo infinito elige un oponente al azar, le resta damage de vida y se suma damage/2 a sí mismo. La pelea se hace dentro de bloques synchronized.

2. **Invariante**: con N y salud inicial H, la suma total debería permanecer constante (salvo durante un update). Calcula ese valor y úsalo para validar.

   > El valor inicial sería N * H (ej: 8 inmortales con 100 de vida = 800). Pero como cada pelea resta damage al otro y solo suma damage/2 al atacante, en cada pelea se pierden damage/2 puntos del total. Entonces la suma total va bajando conforme avanzan las peleas, no se mantiene constante realmente.

3. Ejecuta la UI y prueba **"Pause & Check"**. ¿Se cumple el invariante? Explica.

   > No siempre. Si se usa el modo naive, puede que al pausar algunos hilos estén todavía peleando y los valores queden inconsistentes (un hilo ya restó pero aún no sumó, por ejemplo). Hay condiciones de carrera porque dos hilos pueden estar modificando la salud del mismo inmortal a la vez.

4. **Pausa correcta**: asegura que **todos** los hilos queden pausados **antes** de leer/imprimir la salud; implementa **Resume** (ya disponible).

   > En `PauseController` se usa un `ReentrantLock` con `Condition`. Cuando se llama `pause()`, se pone una bandera en `true`, y cada hilo al inicio de cada iteración llama a `awaitIfPaused()` que lo deja esperando hasta que se haga `resume()`. Además se agregó un `AtomicInteger` (`waitingCount`) que cuenta cuántos hilos ya están pausados. En el `ControlFrame`, después de llamar `pause()`, se espera en un loop hasta que el contador iguale la cantidad de hilos vivos, así nos aseguramos de que todos estén detenidos antes de leer la salud.

5. Haz *click* repetido y valida consistencia. ¿Se mantiene el invariante?

   > Usando el modo ordered, sí se mantiene consistente entre clicks sucesivos (la suma baja de forma gradual por las peleas, sin saltos raros). Con naive puede haber inconsistencias porque las condiciones de carrera corrompen los valores.

6. **Regiones críticas**: identifica y sincroniza las secciones de pelea para evitar carreras; si usas múltiples *locks*, anida con **orden consistente**:
   java
   synchronized (lockA) {
     synchronized (lockB) {
       // ...
     }
   }
   

   > La sección crítica es el método doFight(), donde se modifica la salud de ambos inmortales. Se sincroniza con synchronized sobre los dos inmortales involucrados, usando locks anidados. En fightOrdered se adquieren en orden alfabético por nombre para evitar deadlock.

7. Si la app se **detiene** (posible *deadlock*), usa **jps** y **jstack** para diagnosticar.

   > Si se ejecuta con -Dfight=naive, con suficientes inmortales la app se puede congelar. Con jps se obtiene el PID del proceso Java y con jstack <PID> se ve el dump de hilos. Ahí se vería que dos hilos tienen lock sobre un inmortal y esperan el lock del otro (deadlock circular).

8. Aplica una **estrategia** para corregir el *deadlock* (p. ej., **orden total** por nombre/id, o **tryLock(timeout)** con reintentos y *backoff*).

   > Se usó **orden total por nombre**: antes de hacer synchronized, se compara this.name con other.name y siempre se toma el lock del que viene primero alfabéticamente. Así nunca hay ciclo de espera porque todos los hilos adquieren los locks en el mismo orden.

9. Valida con **N=100, 1000 o 10000** inmortales. Si falla el invariante, revisa la pausa y las regiones críticas.

   > Con ordered, funciona bien con N=100 y N=1000. La suma total va bajando conforme mueren inmortales. No se detectaron inconsistencias ni deadlocks. Con N=10000 tarda más pero sigue funcionando.

10. **Remover inmortales muertos** sin bloquear la simulación: analiza si crea una **condición de carrera** con muchos hilos y corrige **sin sincronización global** (colección concurrente o enfoque *lock-free*).

    > Se cambió el ArrayList por CopyOnWriteArrayList. Cuando un inmortal muere (salud <= 0), se remueve de la lista directamente. CopyOnWriteArrayList crea una copia del arreglo en cada escritura, así que las lecturas no se bloquean. No necesitamos un lock global. En pickOpponent() se captura IndexOutOfBoundsException por si otro hilo removió un inmortal entre el size() y el get().

11. Implementa completamente **STOP** (apagado ordenado).

    > En stop() primero se llama resume() para despertar a los hilos que estén pausados. Después se pone running = false en cada inmortal para que salgan del ciclo, y se llama shutdownNow() en el executor para interrumpir los que estén en sleep. Los hilos capturan la interrupción y terminan limpiamente.

---

## Entregables

1. **Código fuente** (Java 21) con la UI funcionando.  
2. **Informe de laboratorio en formato pdf** con:
   - Parte I: diagnóstico de CPU y cambios para eliminar espera activa.  
   - Parte II: diseño de **parada temprana** y cómo evitas condiciones de carrera en el contador.  
   - Parte III:  
     - Regiones críticas y estrategia adoptada (**orden total** o **tryLock+timeout**).  
     - Evidencia de *deadlock* (si ocurrió) con jstack y corrección aplicada.  
     - Validación del **invariante** con **Pause & Check** (distintos N).  
     - Estrategia para **remover inmortales muertos** sin sincronización global.
3. Instrucciones de ejecución si cambias *defaults*.

---

## Criterios de evaluación (10 pts)

- (3) **Concurrencia correcta**: sin *data races*; sincronización bien localizada; no hay espera activa.  
- (2) **Pausa/Reanudar**: consistencia del estado e invariante bajo **Pause & Check**.  
- (2) **Robustez**: corre con N alto; sin ConcurrentModificationException, sin *deadlocks* no gestionados.  
- (1.5) **Calidad**: arquitectura clara, nombres y comentarios; separación UI/lógica.  
- (1.5) **Documentación**: **RESPUESTAS.txt** claro con evidencia (dumps/capturas) y justificación técnica.

---

## Tips y configuración útil

- **Estrategias de pelea**:  
  - -Dfight=naive → útil para **reproducir** carreras y *deadlocks*.  
  - -Dfight=ordered → **evita** *deadlocks* (orden total por nombre/id).
- **Pausa cooperativa**: usa PauseController (Lock/Condition), **sin** suspend/resume/stop.  
- **Colecciones**: evita estructuras no seguras; prefiere inmutabilidad o colecciones concurrentes.  
- **Diagnóstico**: jps, jstack, **jVisualVM**; revisa *thread dumps* cuando sospeches *deadlock*.  
- **Virtual Threads**: favorecen esperar con bloqueo (no *busy-wait*); usa timeouts.

---

## Cómo correr pruebas

bash
mvn clean verify


Incluye compilación y pruebas JUnit.

---

## Créditos y licencia

Laboratorio basado en el enunciado histórico del curso (Highlander, Productor/Consumidor, Búsqueda distribuida), modernizado a **Java 21**.  
<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a><br />Este contenido hace parte del curso Arquitecturas de Software (ECI) y está licenciado como <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">Creative Commons Attribution-NonCommercial 4.0 International License</a>.
                