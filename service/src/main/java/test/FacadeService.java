package test;

import io.baratine.core.Result;
import io.baratine.core.Service;

@Service("public:///test")
public class FacadeService implements TestService
{
/*
  @Inject
  @Lookup("/test-service1")
  TestService _testService;
*/

  public void test(Result<Boolean> result)
  {
    result.complete(true);
  }
}
