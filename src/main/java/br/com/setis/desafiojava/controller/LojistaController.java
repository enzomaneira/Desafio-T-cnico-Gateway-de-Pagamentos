package br.com.setis.desafiojava.controller;

import br.com.setis.desafiojava.domain.entity.Lojista;
import br.com.setis.desafiojava.dto.lojista.CriarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.EditarLojistaRequest;
import br.com.setis.desafiojava.dto.lojista.ProvedoresRequest;
import br.com.setis.desafiojava.dto.pagina.PaginaResponse;
import br.com.setis.desafiojava.dto.usuarioLojista.CriarUsuarioLojistaRequest;
import br.com.setis.desafiojava.dto.usuarioLojista.EditarUsuarioLojista;
import br.com.setis.desafiojava.dto.usuarioLojista.UsuarioLojistaDto;
import br.com.setis.desafiojava.service.LojistaService;
import br.com.setis.desafiojava.service.UsuarioLojistaService;
import br.com.setis.desafiojava.utils.AuthenticationParseJwt;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${rotas.lojistas}")
public class LojistaController {

  private final LojistaService lojistaService;
  private final UsuarioLojistaService usuarioLojistaService;
  private final AuthenticationParseJwt authenticationParseJwt;

  @Value("${rotas.lojistas}")
  private String rotaLojistas;

  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<Lojista> cadastrarLojista(
      @RequestBody @Valid CriarLojistaRequest request, UriComponentsBuilder uriBuilder) {
    Lojista novoLojista = lojistaService.cadastrarLojista(request);

    URI uri = uriBuilder.path(rotaLojistas + "/{id}").buildAndExpand(novoLojista.getId()).toUri();
    return ResponseEntity.created(uri).body(novoLojista);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<PaginaResponse<Lojista>> listarLojistas(
      @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    Page<Lojista> page = lojistaService.listarLojistas(pageable);
    return ResponseEntity.ok(new PaginaResponse<>(page));
  }

  @GetMapping("{lojistaId}")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<Lojista> listarLojistaPorId(@PathVariable("lojistaId") String lojistaId) {
    return lojistaService
        .listarLojistaPorId(lojistaId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PutMapping("{lojistaId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<Lojista> atualizarLojista(
      @PathVariable("lojistaId") String lojistaId,
      @RequestBody @Valid EditarLojistaRequest lojistaRequest) {
    Lojista lojistaAtualizado = lojistaService.atualizarPorId(lojistaId, lojistaRequest);
    return ResponseEntity.ok().body(lojistaAtualizado);
  }

  @PostMapping("{lojistaId}/usuarios")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<UsuarioLojistaDto> cadastrarUsuarioLojista(
      @RequestBody @Valid CriarUsuarioLojistaRequest request,
      @PathVariable("lojistaId") String lojistaId,
      @AuthenticationPrincipal Jwt jwt,
      UriComponentsBuilder uriBuilder) {
    var cargo = authenticationParseJwt.obterCargoBaseadoEmRoles(jwt);
    UsuarioLojistaDto novoUsuario =
        usuarioLojistaService.cadastrarUsuarioLojista(request, UUID.fromString(lojistaId), cargo);
    URI uri =
        uriBuilder
            .path(rotaLojistas + lojistaId + "/usuarios/{id}")
            .buildAndExpand(novoUsuario.id())
            .toUri();

    return ResponseEntity.created(uri).body(novoUsuario);
  }

  @GetMapping("{lojistaId}/usuarios")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<PaginaResponse<UsuarioLojistaDto>> listarTodosUsuariosPorLojista(
      @PathVariable("lojistaId") String lojistaId,
      @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    Page<UsuarioLojistaDto> page =
        usuarioLojistaService.listarUsuariosPorLojista(pageable, lojistaId);
    return ResponseEntity.ok(new PaginaResponse<>(page));
  }

  @GetMapping("{lojistaId}/usuarios/{usuarioId}")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<UsuarioLojistaDto> listarUsuarioPorLojista(
      @PathVariable("lojistaId") String lojistaId, @PathVariable("usuarioId") String usuarioId) {
    return usuarioLojistaService
        .listarUsuarioPorLojista(usuarioId, lojistaId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PutMapping("{lojistaId}/usuarios/{usuarioId}")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<UsuarioLojistaDto> atualizarUsuarioPorLojista(
      @PathVariable("lojistaId") String lojistaId,
      @PathVariable("usuarioId") String usuarioId,
      @RequestBody @Valid EditarUsuarioLojista request) {
    UsuarioLojistaDto usuarioAtualizado =
        usuarioLojistaService.atualizarUsuarioPorId(usuarioId, lojistaId, request);
    return ResponseEntity.ok().body(usuarioAtualizado);
  }

  @PostMapping("{lojistaId}/provedores")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<Lojista> cadastrarNovosProvedores(
      @PathVariable("lojistaId") String lojistaId,
      @RequestBody @Valid ProvedoresRequest provedores) {
    Lojista lojista = lojistaService.cadastrarNovosProvedores(lojistaId, provedores);
    return ResponseEntity.ok().body(lojista);
  }

  @PostMapping("{lojistaId}/provedores/remover")
  @PreAuthorize("@lojistaSecurity.podeGerenciarLojista(#lojistaId, principal)")
  public ResponseEntity<Lojista> removerProvedores(
      @PathVariable("lojistaId") String lojistaId,
      @RequestBody @Valid ProvedoresRequest provedores) {
    Lojista lojista = lojistaService.removerProvedores(lojistaId, provedores);
    return ResponseEntity.ok().body(lojista);
  }
}
