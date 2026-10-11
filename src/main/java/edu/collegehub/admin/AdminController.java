package edu.collegehub.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceService;
import edu.collegehub.resources.UploadStorageService;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ResourceService resources;
    private final UploadStorageService storage;
    public AdminController(ResourceService resources, UploadStorageService storage) { this.resources = resources; this.storage = storage; }

    @GetMapping("/uploads")
    public String queue(Model model) {
        model.addAttribute("pendingResources", resources.pendingResources());
        return "admin-queue";
    }
    @GetMapping("/uploads/{id}/file")
    public ResponseEntity<byte[]> reviewFile(@PathVariable Long id) throws IOException {
        Resource resource = resources.moderationResource(id);
        byte[] bytes = Files.readAllBytes(storage.pathFor(resource.getStoredFilename()));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(resource.getFileFormat().getMediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(resource.getDisplayFilename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }
    @PostMapping("/uploads/{id}/approve")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal AppUser admin, RedirectAttributes redirect) {
        resources.approve(id, admin);
        redirect.addFlashAttribute("moderationMessage", "The resource is approved and now appears in public browsing.");
        return "redirect:/admin/uploads";
    }
    @PostMapping("/uploads/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam @Size(min = 3, max = 500) String reason,
                         @AuthenticationPrincipal AppUser admin, RedirectAttributes redirect) {
        resources.reject(id, reason, admin);
        redirect.addFlashAttribute("moderationMessage", "The resource was rejected and the reason was sent to the contributor's My uploads page.");
        return "redirect:/admin/uploads";
    }
}
