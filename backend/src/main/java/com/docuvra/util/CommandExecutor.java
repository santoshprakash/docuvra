package com.docuvra.util;

import com.docuvra.dto.CommandResult;
import com.docuvra.exception.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CommandExecutor {

    public CommandResult execute(List<String> command, Duration timeout) {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            process = processBuilder.start();
            Process runningProcess = process;
            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getErrorStream()));

            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new ConversionException("Document preview conversion failed. Please try again or download the original file.");
            }

            CommandResult result = new CommandResult(
                    process.exitValue(),
                    stdout.join(),
                    stderr.join(),
                    process.exitValue() == 0
            );
            log.info("Command completed exitCode={} command={}", result.exitCode(), command);
            if (!result.success()) {
                log.warn("Command failed stdout={} stderr={}", result.stdout(), result.stderr());
            }
            return result;
        } catch (IOException exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }
    }

    private String readStream(java.io.InputStream inputStream) {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }
}
