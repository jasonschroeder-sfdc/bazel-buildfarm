package build.buildfarm.common.config;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @class BackplaneTest
 * @brief Tests utility functions for Backplane configuration overrides
 */
@RunWith(JUnit4.class)
public class BackplaneTest {

    @Before
    public void assertNoEnvVariable() {
        // If a REDIS_PASSWORD env variable is set, it wins. We're not mocking env vars.
        assertThat(System.getenv("REDIS_PASSWORD")).isNull();
    }
    @Test
    public void testRedisPasswordFromUri() {
        Backplane b = new Backplane();
        String testRedisUri = "redis://user:pass1@redisHost.redisDomain";
        b.setRedisUri(testRedisUri);
        assertThat(b.getRedisPassword()).isEqualTo("pass1");
    }

    /**
     * Validate that the redis URI password is higher priority than the `redisPassword` in the config
     */
    @Test
    public void testRedisPasswordPriorities() {
        Backplane b = new Backplane();
        b.setRedisUri("redis://user:pass1@redisHost.redisDomain");
        b.setRedisPassword("pass2");
        assertThat(b.getRedisPassword()).isEqualTo("pass1");
    }
}
