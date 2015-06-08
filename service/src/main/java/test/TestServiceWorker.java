package test;

import io.baratine.core.AfterBatch;
import io.baratine.core.Modify;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

//@Service("public:///test")
@Service("/test-worker")
@Workers(10)
public class TestServiceWorker implements TestService
{
  @Modify
  public void test(Result<Boolean> result)
  {
    result.complete(true);
  }

  @OnSave
  public void save() throws InterruptedException
  {
    Thread.sleep(5);
  }

  @AfterBatch
  public void afterBatch() throws InterruptedException
  {
    Thread.sleep(5);
  }
}
