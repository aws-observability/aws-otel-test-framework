# ------------------------------------------------------------------------
# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
# -------------------------------------------------------------------------

resource "aws_lb" "mocked_server_lb" {
  count = var.disable_mocked_server ? 0 : 1
  # use public subnet to make the lb accessible from public internet
  subnets         = module.basic_components.aoc_public_subnet_ids
  security_groups = [module.basic_components.aoc_security_group_id]
  name            = "aoc-lb-${module.common.testing_id}"
}

resource "aws_lb_target_group" "mocked_server_lb_tg" {
  count       = var.disable_mocked_server ? 0 : 1
  name        = "ms-lbtg-${module.common.testing_id}"
  port        = module.common.mocked_server_http_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = module.basic_components.aoc_vpc_id

  health_check {
    path                = "/"
    unhealthy_threshold = 10
    healthy_threshold   = 2
    interval            = 10
    matcher             = "200,404"
  }
}

resource "aws_lb_listener" "mocked_server_lb_listener" {
  count             = var.disable_mocked_server ? 0 : 1
  load_balancer_arn = aws_lb.mocked_server_lb[0].arn
  port              = module.common.mocked_server_lb_port
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.mocked_server_lb_tg[0].arn
  }
}