package javax.persistence;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.hibernate.ejb.HibernatePersistence;

/**
 * Prevents warning message from Hibernate:
 * <br>WARN org.hibernate.ejb.HibernatePersistence - HHH015016: Encountered a deprecated javax.persistence.spi.PersistenceProvider [org.hibernate.ejb.HibernatePersistence]; use [org.hibernate.jpa.HibernatePersistenceProvider] instead.
 * <br>Copied from http://ask.ttwait.com/que/23041964
 * <br>Alternative solution and open bug at: https://hibernate.atlassian.net/browse/HHH-9141
 * @author FWiers
 *
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public class CustomPersistence extends Persistence {

    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
        return CustomPersistence.createEntityManagerFactory(persistenceUnitName, null);
    }

    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
        
    	EntityManagerFactory emf = null;
        List<PersistenceProvider> providers = getProviders();
        PersistenceProvider defaultProvider = null;
        for (PersistenceProvider provider : providers) {
            if (provider instanceof HibernatePersistence) {
                defaultProvider = provider;
                continue;
            }
            emf = provider.createEntityManagerFactory(persistenceUnitName, properties);
            if (emf != null) {
                break;
            }
        }
        if (emf == null && defaultProvider != null)
            emf = defaultProvider.createEntityManagerFactory( persistenceUnitName, properties );
        if ( emf == null ) {
            throw new PersistenceException( "No Persistence provider for EntityManager named " + persistenceUnitName );
        }
        return emf;
    }

    protected static List<PersistenceProvider> getProviders() {
        return PersistenceProviderResolverHolder
                .getPersistenceProviderResolver()
                .getPersistenceProviders();
    }

}