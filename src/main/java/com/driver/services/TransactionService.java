package com.driver.services;

import com.driver.models.*;
import com.driver.repositories.BookRepository;
import com.driver.repositories.CardRepository;
import com.driver.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    public int max_allowed_books;

    @Value("${books.max_allowed_days}")
    public int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    public int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        Book book = bookRepository5.findById(bookId).get();
        Card card = cardRepository5.findById(cardId).get();

        if(book==null || !book.isAvailable()){
            throw new Exception("Book is either unavailable or not present");
        }else if(card==null || card.getCardStatus().equals(CardStatus.DEACTIVATED)){
            throw new Exception("Card is invalid");
        }else if(card.getBooks().size()>=max_allowed_books){
            throw new Exception("Book limit has reached for this card");
        }
        book.setAvailable(false);
        book.setCard(card);
        bookRepository5.updateBook(book);

        Transaction transaction = Transaction.builder()
                .card(card)
                .transactionId(UUID.randomUUID().toString())
                .isIssueOperation(true)
                .fineAmount(0)
                .book(book)
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .build();

        transactionRepository5.save(transaction);
        //Note that the error message should match exactly in all cases

       return transaction.getTransactionId(); //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId, TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);
        int fine = 0;
        Date today = new Date();

        Date issueDate = transaction.getTransactionDate();
        int days = calculateDays(new java.sql.Date(today.getTime()).toLocalDate(),new java.sql.Date(issueDate.getTime()).toLocalDate());
        if(days>getMax_allowed_days){
            fine = fine_per_day*(days-getMax_allowed_days);
        }
        Transaction tx = Transaction.builder()
                .fineAmount(fine)
                .transactionId(UUID.randomUUID().toString())
                .book(transaction.getBook())
                .card(transaction.getCard())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .isIssueOperation(false)
                .build();
        Book book = transaction.getBook();
        book.setCard(null);
        book.setAvailable(true);
        bookRepository5.updateBook(book);
        tx.setBook(book);
        tx.setCard(cardRepository5.findById(cardId).get());
        transactionRepository5.save(tx);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        return tx; //return the transaction after updating all details
    }

    public int calculateDays(LocalDate today, LocalDate issueDate){
        return (int)ChronoUnit.DAYS.between(today,issueDate);
    }
}
