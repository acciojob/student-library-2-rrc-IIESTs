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
import java.util.Date;
import java.util.List;

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
        Transaction transaction = new Transaction();
        String s = "transaction failed";
           boolean isPresent= bookRepository5.existsById(bookId);
           if(isPresent){
              Book book =  bookRepository5.findById(bookId).get();
              if(!book.isAvailable()){
                  transaction.setIssueOperation(false);
                  transaction.setTransactionStatus(TransactionStatus.FAILED);
                  throw new Exception("Book is either unavailable or not present");
              }
           }else{
               transaction.setIssueOperation(false);
               transaction.setTransactionStatus(TransactionStatus.FAILED);
               throw new Exception("Book is either unavailable or not present");
           }
           if(cardRepository5.existsById(cardId)){
               Card card = cardRepository5.findById(cardId).get();
               if(card.getCardStatus().equals(CardStatus.DEACTIVATED)){
                   transaction.setIssueOperation(false);
                   transaction.setTransactionStatus(TransactionStatus.FAILED);
                   throw new Exception("Card is invalid");
               }
           }else{
               transaction.setIssueOperation(false);
               transaction.setTransactionStatus(TransactionStatus.FAILED);
               throw new Exception("Card is invalid");
           }

            Card card = cardRepository5.findById(cardId).get();

            List<Book> books = card.getBooks();
            if(books.size()>=max_allowed_books){
                transaction.setIssueOperation(false);
                transaction.setTransactionStatus(TransactionStatus.FAILED);
                throw new Exception("Book limit has reached for this card");
            }
            Book book = bookRepository5.findById(bookId).get();
            book.setCard(card);
            book.setAvailable(false);
            bookRepository5.updateBook(book);
            transaction.setBook(book);
            transaction.setCard(card);
            transaction.setIssueOperation(true);
            transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
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
        if(days>0){
            fine = fine_per_day*days;
        }

        Book book = bookRepository5.findById(bookId).get();
        book.setCard(null);
        book.setAvailable(true);
        bookRepository5.updateBook(book);
        Transaction tx = new Transaction();
        tx.setBook(book);
        tx.setCard(cardRepository5.findById(cardId).get());
        tx.setIssueOperation(false);
        tx.setTransactionStatus(TransactionStatus.SUCCESSFUL);
        tx.setFineAmount(fine);
        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        return tx; //return the transaction after updating all details
    }

    public int calculateDays(LocalDate today, LocalDate issueDate){
        return (int)ChronoUnit.DAYS.between(today,issueDate);
    }
}
