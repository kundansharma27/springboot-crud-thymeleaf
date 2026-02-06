package com.boostmytool.beststore.controllers;

import com.boostmytool.beststore.models.Product;
import com.boostmytool.beststore.models.ProductDto;
import com.boostmytool.beststore.services.ProductsRepository;

import java.io.IOException;
import java.nio.file.Path;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.Binding;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Date;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository repo;

    @GetMapping({"","/"})
    public String showProductList(Model model) {
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC,"id"));
        model.addAttribute("products", products);
        return "products/index";

    }
    @GetMapping("/create")
    public String showCreatePage(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto",productDto);
        return "products/CreateProduct";
    }
   @PostMapping("/create")
    public String  createProduct(
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result
   ) {
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto","imageFile", "The image File is required"));
            }
        if (result.hasErrors()){
            return "products/CreateProduct";
        }
        // Save the image file
       MultipartFile image = productDto.getImageFile();
        Date createdAt = new Date();
        String storageFileName = createdAt.getTime()+"_"+ image.getOriginalFilename();

        try {
            String uploadDir = "src/main/resources/static/images/";

            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream,Paths.get(uploadDir + storageFileName),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
        }
       Product product = new Product();
       product.setName(productDto.getName());
       product.setBrand(productDto.getBrand());
       product.setCategory(productDto.getCategory());
       product.setPrice(productDto.getPrice());
       product.setDescription(productDto.getDescription());
       product.setImageFileName(storageFileName);
       product.setCreatedAt(createdAt);

       repo.save(product);


       return "redirect:/products";
   }

   @GetMapping("/edit")
    public String showEditPage(
            Model model,
            @RequestParam int id
   ){

   try {
       Product product = repo.findById(id).get();
       model.addAttribute("product",product);

       ProductDto productDto = new ProductDto();
       productDto.setName(product.getName());
       productDto.setBrand(product.getBrand());
       productDto.setCategory(product.getCategory());
       productDto.setPrice(product.getPrice());
       productDto.setDescription(product.getDescription());

       model.addAttribute("productDto", productDto);
   } catch (Exception ex) {
       System.out.println("Exception:" + ex.getMessage());
       return "redirect:/products";
   }
        return "products/EditProduct";
   }

   @PostMapping("/edit")
    public String updateProduct(
            Model model,
            @RequestParam int id,
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result
           ){

        try {
            Product product = repo.findById(id).get();
            model.addAttribute("product",product);

            if (result.hasErrors()){
                return "products/EditProduct";
            }

            // 3️⃣ If new image uploaded → delete old + save new
            if (!productDto.getImageFile().isEmpty()) {

                // 🔴 DELETE OLD IMAGE
                String uploadDir = "src/main/resources/static/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                try {
                    Files.deleteIfExists(oldImagePath);
                } catch (Exception ex) {
                    System.out.println("Could not delete old image: " + ex.getMessage());
                }

                // 🟢 SAVE NEW IMAGE
                MultipartFile image = productDto.getImageFile();
                Date createdAt = new Date();
                String newImageName =
                        createdAt.getTime() + "_" + image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(
                            inputStream,
                            Paths.get(uploadDir + newImageName),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (Exception ex) {
                    System.out.println("Image upload failed: " + ex.getMessage());
                }

                // Update image name in product
                product.setImageFileName(newImageName);
            }

            // 4️⃣ Update remaining fields
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());

            // 5️⃣ Save updated product
            repo.save(product);


        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
        }


        return "redirect:/products";
   }

    @GetMapping("/delete")
    public String deleteProduct(
            @RequestParam int id
            ) {
        try {
            Product product = repo.findById(id).get();

            // delete product image
            Path imagePath = Paths.get("public/images/" + product.getImageFileName());
            try {
                Files.delete(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception:" + ex.getMessage());
            }

            // delete the product
            repo.delete(product);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return "redirect:/products";
    }

}
