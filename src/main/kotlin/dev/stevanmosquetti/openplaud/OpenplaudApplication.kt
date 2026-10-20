package dev.stevanmosquetti.openplaud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenplaudApplication

fun main(args: Array<String>) {
	runApplication<OpenplaudApplication>(*args)
}
