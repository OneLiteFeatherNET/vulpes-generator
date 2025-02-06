package net.theevilreaper.vulpes.generator.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;
import net.theevilreaper.vulpes.api.model.ItemModel;
import net.theevilreaper.vulpes.api.repository.ItemRepository;

@Controller("/test")
public class Test {

    private final ItemRepository itemRepository;

    @Inject
    public Test(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Get()
    public HttpResponse<String> test() {
        System.out.println("Test");
        for (ItemModel itemModel : itemRepository.findAll()) {
            System.out.println(itemModel);
        }
        return HttpResponse.ok("Test");
    }
}
