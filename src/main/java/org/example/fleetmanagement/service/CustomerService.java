package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Customer;
import org.example.fleetmanagement.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for customer operations.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    // Constructor injection of the customer repository.
    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Returns all customers.
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Saves a new customer.
    public Customer addCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Persists changes to an existing customer.
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Deletes a customer by id.
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
}
