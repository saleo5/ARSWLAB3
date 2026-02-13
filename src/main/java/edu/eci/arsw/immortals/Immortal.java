package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Immortal implements Runnable {
  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private final String fightMode;
  private volatile boolean running = true;

  public Immortal(String name, int health, int damage, List<Immortal> population,
                  ScoreBoard scoreBoard, PauseController controller, String fightMode) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
    this.fightMode = Objects.requireNonNull(fightMode);
  }

  public String name() { return name; }
  public synchronized int getHealth() { return health; }
  public boolean isAlive() { return getHealth() > 0 && running; }
  public void stop() { running = false; }

  @Override public void run() {
    try {
      while (running) {
        controller.awaitIfPaused();
        if (!running) break;
        var opponent = pickOpponent();
        if (opponent == null) continue;
        if ("naive".equalsIgnoreCase(fightMode)) fightNaive(opponent);
        else fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private Immortal pickOpponent() {
    try {
      int size = population.size();
      if (size <= 1) return null;
      Immortal other;
      do {
        other = population.get(ThreadLocalRandom.current().nextInt(size));
      } while (other == this);
      return other;
    } catch (IndexOutOfBoundsException e) {
      // puede pasar si otro hilo eliminó un inmortal entre size() y get()
      return null;
    }
  }

  // toma los locks en orden arbitrario (puede causar deadlock)
  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        doFight(other);
      }
    }
  }

  // se ordenan los locks por nombre para evitar deadlock
  private void fightOrdered(Immortal other) {
    Immortal first, second;
    if (this.name.compareTo(other.name) < 0) {
      first = this;
      second = other;
    } else {
      first = other;
      second = this;
    }
    synchronized (first) {
      synchronized (second) {
        doFight(other);
      }
    }
  }

  private void doFight(Immortal other) {
    if (this.health <= 0 || other.health <= 0) return;
    other.health -= this.damage;
    this.health += this.damage / 2;
    scoreBoard.recordFight();
    // si el oponente muere, lo sacamos de la lista
    if (other.health <= 0) {
      population.remove(other);
    }
  }
}
