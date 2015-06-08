package test;

import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.Result;
import io.baratine.core.Service;

import javax.inject.Inject;

//@Service("public:///test")
@Service("public:///test-service1")
public class TestService1   implements TestService
{
  @Inject
  @Lookup("/test-service2")
  TestService _testService;


  @Modify
  public void test(Result<Boolean> result)
  {
    _testService.test(result);
  }
}
