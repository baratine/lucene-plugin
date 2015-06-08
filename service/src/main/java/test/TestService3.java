package test;

import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.Result;
import io.baratine.core.Service;

import javax.inject.Inject;

//@Service("public:///test")
@Service("public:///test-service3")
public class TestService3 implements TestService
{
  @Inject
  @Lookup("/test-worker")
  TestService _worker;

  @Modify
  public void test(Result<Boolean> result)
  {
    _worker.test(result);
  }
}
