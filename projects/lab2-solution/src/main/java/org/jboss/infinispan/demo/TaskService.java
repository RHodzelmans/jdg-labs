package org.jboss.infinispan.demo;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.jboss.infinispan.demo.model.Task;

@Stateless
public class TaskService {

	@PersistenceContext
    EntityManager em;
	
	@Inject
	Cache<Long,Task> cache;
	
	Logger log = Logger.getLogger(this.getClass().getName());

	/**
	 * This methods should return all cache entries, currently contains mockup code. 
	 * @return
	 */
	public Collection<Task> findAll() {
		return cache.values();
	}
	
	/**
	 * This method filters task based on the input
	 * @param input - string to filter on
	 * @return
	 * 
	 * DONE: The current implementation is database query, replace it with a JDG query instead
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<Task> filter(String input) {
		SearchManager sm = Search.getSearchManager(cache);
		QueryBuilder qb = sm.getSearchFactory().buildQueryBuilder().forEntity(Task.class).get();
		Query q = qb.keyword().onField("title").matching(input).createQuery();
		CacheQuery cq = sm.getQuery(q, Task.class);
		return (Collection<Task>)(List)cq.list(); //Since we only Query Task.class we can safely cast this
	}

	/**
	 * This method persists a new Task instance
	 * @param task
	 * 
	 */
	public void insert(Task task) {
		if(task.getCreatedOn()==null)
			task.setCreatedOn(new Date());
		em.persist(task);
		cache.put(task.getId(),task);
	}


	/**
	 * This method persists an existing Task instance
	 * @param task
	 * 
	 */
	public void update(Task task) {
		Task newTask = em.merge(task);
		em.detach(newTask);
		cache.replace(task.getId(),newTask);
	}
	
	/**
	 * This method deletes an Task from the persistence store
	 * @param task
	 * 
	 */
	public void delete(Task task) {
		//Note object may be detached so we need to tell it to remove based on reference
		em.remove(em.getReference(task.getClass(),task.getId()));
		cache.remove(task.getId());
	}
	
	
	/**
	 * This method is called after construction of this SLSB.
	 * 
	 */
	@PostConstruct
	public void startup() {
		
		log.info("### Querying the database for tasks!!!!");
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Task> criteriaQuery = criteriaBuilder.createQuery(Task.class);
	
		Root<Task> root = criteriaQuery.from(Task.class);
		criteriaQuery.select(root);
		Collection<Task> resultList = em.createQuery(criteriaQuery).getResultList();
		
		for (Task task : resultList) {
			this.insert(task);
		}
		
	}
	
}
