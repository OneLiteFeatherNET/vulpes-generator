package net.onelitefeather.vulpes.generator.controller;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import net.onelitefeather.vulpes.generator.domain.GenerateCodeResponse;
import net.onelitefeather.vulpes.generator.service.GitService;
import net.onelitefeather.vulpes.generator.service.RepositoryInitializerService;

import java.util.List;

@Controller("/references")
public class ReferencesController {

    private final GitService gitService;
    private final RepositoryInitializerService repositoryInitializerService;
    private final String gitRepoUrl;

    @Inject
    public ReferencesController(
            GitService gitService, 
            RepositoryInitializerService repositoryInitializerService,
            @Value("${git.repo.url:https://github.com/yourusername/your-repo.git}") String gitRepoUrl
    ) {
        this.gitService = gitService;
        this.repositoryInitializerService = repositoryInitializerService;
        this.gitRepoUrl = gitRepoUrl;
    }

    @Operation(
            summary = "List branches",
            description = "Lists all available branches in the repository",
            tags = {"References"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of branches retrieved successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = List.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Error retrieving branches",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeErrorDTO.class)
            )
    )
    @Get("/branches")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<List<String>> listBranches() {
        repositoryInitializerService.updateRepository();
        List<String> branches = gitService.listBranches(gitRepoUrl);
        return HttpResponse.ok(branches);
    }

    @Operation(
            summary = "List tags",
            description = "Lists all available tags in the repository",
            tags = {"References"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of tags retrieved successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = List.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Error retrieving tags",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeErrorDTO.class)
            )
    )
    @Get("/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<List<String>> listTags() {
        repositoryInitializerService.updateRepository();
        List<String> tags = gitService.listTags(gitRepoUrl);
        return HttpResponse.ok(tags);
    }

    @Operation(
            summary = "List all references",
            description = "Lists all available references (branches and tags) in the repository",
            tags = {"References"}
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of references retrieved successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = List.class)
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Error retrieving references",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateCodeResponse.GenerateCodeErrorDTO.class)
            )
    )
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<List<String>> listReferences() {
        repositoryInitializerService.updateRepository();
        List<String> references = gitService.listReferences(gitRepoUrl);
        return HttpResponse.ok(references);
    }
}
