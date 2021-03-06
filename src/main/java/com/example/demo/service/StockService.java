package com.example.demo.service;

import com.example.demo.dao.StockInfo;
import com.example.demo.domain.Stock;
import com.example.demo.domain.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

@Service
public class StockService {
    public static final Logger logger = LoggerFactory.getLogger(StockService.class);

    @Resource(name = "stockRepository")
    private StockRepository stockRepository;

    @Autowired
    private StockInfo stockInfo;

    @Value("${kosdaqUrl}")
    private String kosdaqUrl;

    @Value("${kospiUrl}")
    private String kospiUrl;

    @PersistenceContext
    private EntityManager em;

    @Value("${batch.size}")
    private Integer batchSize;

    private static final double maxHighPercent = 30.0;
    private static final double maxLowPercent = -30.0;
    private static final int minCost = 0;

    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    public Stock getStockById(long id) {
        return stockRepository.findOne(id);
    }

    public Stock getStockByStockName(String stockName) {
        logger.info("stockName on Service : {}", stockName);
        return stockRepository.findByName(stockName);
    }

    public boolean checkMakingStock(String stockName) {
        Stock stock = stockRepository.findByName(stockName.toUpperCase());
        if (stock != null)
            return true;
        logger.info("주식이 DB에 없음");
        return false;
    }

    @Transactional
    public void deleteStockById(long id) throws Exception {
        logger.info("deleteStockById method called {}", id);
        stockRepository.delete(id);
    }

    @Transactional
    public void getAllStock() throws Exception {
        long start = System.currentTimeMillis();
        stockInfo.getAllStockByUrl(kospiUrl);
        stockInfo.getAllStockByUrl(kosdaqUrl);
        long end = System.currentTimeMillis();
        logger.info("총 걸린 시간 : {}초", (end - start)/1000.0);
    }

    @Transactional
    public Stock updateByStockName(String stockName) throws IOException {
        logger.info("updateByStockName method called on service : {}", stockName);
        Stock stock = stockRepository.findByName(stockName);
        logger.info("{}", stock.getUpdateUrl());
        return stock.update(stockInfo.updateByStockName(stock.getUpdateUrl()));
    }

    @Transactional
    public void detailWholeUpdate() throws IOException {
        long start =  System.currentTimeMillis();
        List<Stock> original = stockRepository.findAll();
        for (int i = 0; i  < 100; i++) {
            Stock stock = em.merge(original.get(i).update(stockInfo.updateByStockName(original.get(i).getUpdateUrl())));
            em.persist(stock);
            if (i % batchSize == 0) {
                logger.info("{}번째 배치 업데이트 실행", i);
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
        long end = System.currentTimeMillis();
        logger.info("총 업데이트 시간 : {}초", (end - start)/1000.0);
    }

    public List<Stock> lowRate() {
        return stockRepository.findTop20ByRateGreaterThanEqualOrderByRateAsc(maxLowPercent);
    }

    public List<Stock> lowCost() {
        return stockRepository.findTop20ByCostGreaterThanOrderByCostAsc(minCost);
    }

    public List<Stock> topRate() {
        return stockRepository.findTop20ByRateLessThanEqualOrderByRateDesc(maxHighPercent);
    }

    public List<Stock> topCost() {
        return stockRepository.findTop20ByOrderByCostDesc();
    }

    public List<Stock> searchByStockName(String stockName) {
        return stockRepository.findByNameStartingWith(stockName);
    }

    public List<Stock> searchByStockNameAndOverCost(String stockName, Integer cost) {
        return stockRepository.findByNameIsStartingWithAndCostGreaterThanEqualOrderByCostDesc(stockName, cost);
    }

    public List<Stock> searchByOverRate(Double overRate) {
        logger.info("overRate : {}", overRate);
        return stockRepository.findByRateGreaterThanEqualOrderByRateDesc(overRate);
    }

    public List<Stock> searchByUnderRate(Double underRate) {
        logger.info("under : {}", underRate);
        return stockRepository.findByRateLessThanEqualOrderByRateDesc(underRate);
    }
}

