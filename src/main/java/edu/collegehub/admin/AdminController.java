package edu.collegehub.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceNotFoundException;
import edu.collegehub.resources.ResourceService;
import edu.collegehub.resources.UploadStorageService;
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
        try {
            resources.approve(id, admin);
            redirect.addFlashAttribute("moderationMessage", "The resource is approved and now appears in public browsing.");
        } catch (ResourceNotFoundException ex) {
            redirect.addFlashAttribute("moderationError", "That submission is no longer pending or available for review. Refresh the queue and try again.");
        }
        return "redirect:/admin/uploads";
    }
    @PostMapping("/uploads/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUser admin, RedirectAttributes redirect) {
        try {
            resources.reject(id, reason, admin);
            redirect.addFlashAttribute("moderationMessage", "The resource was rejected and the reason was sent to the contributor's My uploads page.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("moderationError", ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            redirect.addFlashAttribute("moderationError", "That submission is no longer pending or available for review. Refresh the queue and try again.");
        }
        return "redirect:/admin/uploads";
    }

    @PostMapping("/uploads/{id}/remove")
    public String remove(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUser admin, RedirectAttributes redirect) {
        try {
            resources.removeByAdmin(id, reason, admin);
            redirect.addFlashAttribute("moderationMessage", "The resource was removed from public access. The uploader can see your reason in My uploads.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("moderationError", ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            redirect.addFlashAttribute("moderationError", "That resource is already removed or no longer exists.");
        }
        return "redirect:/admin/uploads";
    }
}
