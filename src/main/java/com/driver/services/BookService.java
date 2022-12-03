package com.driver.services;

import com.driver.models.Author;
import com.driver.models.Book;
import com.driver.repositories.AuthorRepository;
import com.driver.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {


    @Autowired
    BookRepository bookRepository2;

    @Autowired
    AuthorRepository authorRepository;

    public void createBook(Book book){

        bookRepository2.save(book);

    }

    public List<Book> getBooks(String genre, boolean available, String author){
        List<Book> books = null;
        if(author==null){
            books = bookRepository2.findBooksByGenre(genre, true);
        } else if (!available) {
            books = bookRepository2.findBooksByGenreAuthor(genre,author,false);
        }
        //find the elements of the list by yourself
        return books;
    }
}
