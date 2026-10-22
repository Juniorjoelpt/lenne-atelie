package br.com.mssolucoes.atelie.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regra(RegraNegocioException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return erro(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Verifique os campos informados." : msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> leitura(HttpMessageNotReadableException e) {
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique datas, números e opções.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credenciais(BadCredentialsException e) {
        return erro(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> negado(AccessDeniedException e) {
        return erro(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> upload(MaxUploadSizeExceededException e) {
        return erro(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 8 MB.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> semRecurso(NoResourceFoundException e) {
        return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> geral(Exception e) {
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente.");
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
