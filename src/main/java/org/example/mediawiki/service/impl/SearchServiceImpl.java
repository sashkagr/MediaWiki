package org.example.mediawiki.service.impl;

import org.example.mediawiki.cache.Cache;
import org.example.mediawiki.controller.WikiApiRequest;
import org.example.mediawiki.modal.Pages;
import org.example.mediawiki.modal.Search;
import org.example.mediawiki.modal.Word;
import org.example.mediawiki.repository.SearchRepository;
import org.example.mediawiki.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class SearchServiceImpl implements Service<Search> {

    private Cache cache = new Cache();

    private final SearchRepository searchRepository;
    private final PagesServiceImpl pagesService;

    @Autowired
    public SearchServiceImpl(final PagesServiceImpl pages,
                             final SearchRepository search) {
        this.pagesService = pages;
        this.searchRepository = search;

    }

    @Transactional
    public boolean getSearchExistingById(final Long id) {
        for (String key : cache.getCache().keySet()) {
            for (Search element : (List<Search>) cache.getCache().get(key)) {
                if (element.getId() == id) {
                    return true;
                }
            }
        }
        Search search = searchRepository.existingById(id);
        return search != null;

    }

    @Transactional
    public Search getSearchByTitle(final String title) {
        for (String key : cache.getCache().keySet()) {
            for (Search element : (List<Search>) cache.getCache().get(key)) {
                if (element.getTitle() == title) {
                    return element;
                }
            }
        }

        Search search = searchRepository.existingByTitle(title);
        if (search != null) {
            List<Search> searches = new ArrayList<>();
            searches.add(search);
            cache.remove(Long.toString(search.getId()));
            cache.put(Long.toString(search.getId()), searches);
        }
        return search;
    }

    @Transactional
    public Search getSearchById(final Long id) {
        for (String key : cache.getCache().keySet()) {
            for (Search element : (List<Search>) cache.getCache().get(key)) {
                if (element.getId() == id) {
                    return element;
                }
            }
        }
        Search search = searchRepository.existingById(id);
        List<Search> searches = new ArrayList<>();
        Object cachedData = cache.get(Long.toString((search.getId())));
        if (cachedData != null) {
            cache.remove(Long.toString((search.getId())));
            searches = (List<Search>) cachedData;
        }
        searches.add(search);
        cache.put((Long.toString((search.getId()))), searches);

        return search;
    }

    @Override
    @Transactional
    public void create(final Search entity) {
        searchRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(final Long id) {
        for (String key : cache.getCache().keySet()) {
            List<Search> searches = (List<Search>) cache.getCache().get(key);
            for (Search element : searches) {
                if (element.getId() == id) {
                    searches.remove(element);
                    cache.remove(key);
                    cache.put(key, searches);
                    break;
                }
            }
        }
        searchRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void update(final Search entity) {
        for (String key : cache.getCache().keySet()) {
            List<Search> searches = (List<Search>) cache.getCache().get(key);
            for (Search element : searches) {
                if (element.getId() == entity.getId()) {
                    searches.remove(element);
                    cache.remove(key);
                    cache.put(key, searches);
                    break;
                }
            }
        }
        searchRepository.save(entity);
    }

    @Override
    @Transactional
    public List<Search> read() {
        cache.clear();
        List<Search> searches = new ArrayList<>();
        searches = searchRepository.findAll();
        for (Search search : searches) {
            List<Search> searchList = (List<Search>) cache.
                    get((Long.toString(search.getId())));
            if (searchList != null) {
                cache.remove((Long.toString(search.getId())));
                searchList.add(search);
            }
            cache.put((Long.toString(search.getId())), searches);
        }
        return searches;

    }

    public List<Word> createSearchAndPages(final String name) {
        List<Word> words = WikiApiRequest.getDescriptionByTitle(name);
        Search search = new Search();
        search.setTitle(name);
        this.create(search);
        for (Word word : words) {
            Pages page = new Pages();
            word.setSearch(search);
            word.setDescription(word.getDescription().
                    replaceAll("\\<[^\\\\>]*+\\>", ""));
            page.setPageId(word.getId());
            page.setTitle(word.getTitle());
            Pages existingPage = pagesService.
                    getPageByPageId(page.getPageId());
            if (existingPage != null) {
                existingPage.getSearches().add(search);
                pagesService.update(existingPage);
            } else {
                page.getSearches().add(search);
                pagesService.create(page);
            }
        }
        return words;
    }


}
